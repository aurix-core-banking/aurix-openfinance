package com.aurix.platform.openfinance.pipeline;

import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import com.aurix.platform.openfinance.pipeline.canonicalization.service.CanonicalizationService;
import com.aurix.platform.openfinance.pipeline.lineage.entity.LineageRecord;
import com.aurix.platform.openfinance.pipeline.lineage.service.LineageService;
import com.aurix.platform.openfinance.pipeline.lineage.service.LineageService.LineageRequest;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult;
import com.aurix.platform.openfinance.pipeline.pii.service.PiiClassificationService;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityResult;
import com.aurix.platform.openfinance.pipeline.quality.service.DataQualityService;
import com.aurix.platform.openfinance.pipeline.validation.entity.ValidationResult;
import com.aurix.platform.openfinance.pipeline.validation.service.SchemaValidationService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orquestrador do pipeline de dados do Open Finance.
 * Executa o fluxo completo: bruto → canônico → validado → qualidade → PII → linhagem → publicado.
 *
 * Fluxo:
 * 1. Canonicalização — converte dados brutos para formato padrão
 * 2. Validação de Schema — verifica conformidade com schema registrado
 * 3. Qualidade de Dados — aplica regras de completude/consistência/acurácia
 * 4. Classificação PII — identifica e protege dados pessoais
 * 5. Linhagem — registra cadeia de rastreabilidade (INV03)
 * 6. Publicação — retorna resultado para publicação
 */
@Service
public class PipelineOrchestrator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PipelineOrchestrator.class);
    private static final String DEFAULT_SCHEMA_VERSION = "1.0";
    private static final int MIN_QUALITY_SCORE = 80;

    private final CanonicalizationService canonicalizationService;
    private final SchemaValidationService schemaValidationService;
    private final DataQualityService dataQualityService;
    private final PiiClassificationService piiClassificationService;
    private final LineageService lineageService;

    /**
     * Construtor com injeção de dependência.
     *
     * @param canonicalizationService  serviço de canonicalização.
     * @param schemaValidationService  serviço de validação de schema.
     * @param dataQualityService       serviço de qualidade.
     * @param piiClassificationService serviço de PII.
     * @param lineageService           serviço de linhagem.
     */
    public PipelineOrchestrator(final CanonicalizationService canonicalizationService,
            final SchemaValidationService schemaValidationService,
            final DataQualityService dataQualityService,
            final PiiClassificationService piiClassificationService,
            final LineageService lineageService) {
        this.canonicalizationService = canonicalizationService;
        this.schemaValidationService = schemaValidationService;
        this.dataQualityService = dataQualityService;
        this.piiClassificationService = piiClassificationService;
        this.lineageService = lineageService;
    }

    /**
     * Executa o pipeline completo de processamento de dados.
     *
     * @param raw             registro bruto de entrada.
     * @param type            tipo de recurso.
     * @param executionPlanId ID do plano de execução.
     * @return resultado do pipeline.
     */
    public PipelineResult execute(final RawRecord raw, final ResourceType type,
            final String executionPlanId) {
        log.info("Iniciando pipeline: recordId={}, tipo={}, plan={}",
                raw.getRecordId(), type, executionPlanId);

        String pipelineExecutionId = UUID.randomUUID().toString();

        // 1. Canonicalização
        CanonicalRecord canonical;
        try {
            canonical = canonicalizationService.canonicalize(raw, type);
            log.info("Etapa 1/6 - Canonicalização concluída: canonicalId={}",
                    canonical.getCanonicalId());
        } catch (Exception e) {
            log.error("Falha na canonicalização: {}", e.getMessage());
            return PipelineResult.falha(EtapaPipeline.CANONICALIZATION,
                    "Falha na canonicalização: " + e.getMessage());
        }

        // 2. Validação de Schema
        ValidationResult validation;
        try {
            validation = schemaValidationService.validate(canonical, DEFAULT_SCHEMA_VERSION);
            log.info("Etapa 2/6 - Validação concluída: válido={}", validation.isValid());
            if (!validation.isValid()) {
                return PipelineResult.falhaValidacao(validation);
            }
        } catch (Exception e) {
            log.error("Falha na validação de schema: {}", e.getMessage());
            return PipelineResult.falha(EtapaPipeline.VALIDATION,
                    "Falha na validação: " + e.getMessage());
        }

        // 3. Qualidade de Dados
        QualityResult quality;
        try {
            quality = dataQualityService.checkQuality(canonical, type);
            log.info("Etapa 3/6 - Qualidade verificada: pontuação={}, aprovado={}",
                    quality.getScore(), quality.isPassed());
            if (quality.getScore() < MIN_QUALITY_SCORE) {
                return PipelineResult.falhaQualidade(quality);
            }
        } catch (Exception e) {
            log.error("Falha na verificação de qualidade: {}", e.getMessage());
            return PipelineResult.falha(EtapaPipeline.QUALITY,
                    "Falha na verificação de qualidade: " + e.getMessage());
        }

        // 4. Classificação PII
        PiiResult pii;
        try {
            pii = piiClassificationService.classify(canonical);
            log.info("Etapa 4/6 - PII classificado: nível={}, campos={}",
                    pii.getSensitivityLevel(), pii.getProtectedFields().size());
        } catch (Exception e) {
            log.error("Falha na classificação PII: {}", e.getMessage());
            return PipelineResult.falha(EtapaPipeline.PII,
                    "Falha na classificação PII: " + e.getMessage());
        }

        // 5. Linhagem (INV03: Sem linhagem, sem publicação)
        LineageRecord lineage;
        try {
            LineageRequest request = new LineageRequest(
                    null, null, executionPlanId, null,
                    pipelineExecutionId, raw.getRecordId(),
                    canonical.getCanonicalId(), null);
            lineage = lineageService.registerLineage(request);
            log.info("Etapa 5/6 - Linhagem registrada: lineageId={}", lineage.getLineageId());
        } catch (Exception e) {
            log.error("Falha no registro de linhagem: {}", e.getMessage());
            return PipelineResult.falha(EtapaPipeline.LINEAGE,
                    "Falha na linhagem: " + e.getMessage());
        }

        // 6. Publicação (sucesso)
        log.info("Pipeline concluído com sucesso: canonicalId={}, lineageId={}",
                canonical.getCanonicalId(), lineage.getLineageId());
        return PipelineResult.sucesso(canonical, pii, lineage, quality);
    }

    /**
     * Resultado do pipeline de processamento.
     */
    public static class PipelineResult {
        private final boolean sucesso;
        private final CanonicalRecord canonicalRecord;
        private final PiiResult piiResult;
        private final LineageRecord lineageRecord;
        private final QualityResult qualityResult;
        private final ValidationResult validationResult;
        private final EtapaPipeline etapaFalha;
        private final String mensagemFalha;
        private final LocalDateTime executadoEm;

        /**
         * Construtor privado.
         */
        private PipelineResult(final boolean sucesso, final CanonicalRecord canonicalRecord,
                final PiiResult piiResult, final LineageRecord lineageRecord,
                final QualityResult qualityResult, final ValidationResult validationResult,
                final EtapaPipeline etapaFalha, final String mensagemFalha) {
            this.sucesso = sucesso;
            this.canonicalRecord = canonicalRecord;
            this.piiResult = piiResult;
            this.lineageRecord = lineageRecord;
            this.qualityResult = qualityResult;
            this.validationResult = validationResult;
            this.etapaFalha = etapaFalha;
            this.mensagemFalha = mensagemFalha;
            this.executadoEm = LocalDateTime.now();
        }

        /**
         * Cria um resultado de sucesso.
         *
         * @param canonical   registro canônico.
         * @param pii         resultado PII.
         * @param lineage     registro de linhagem.
         * @param quality     resultado de qualidade.
         * @return resultado de sucesso.
         */
        public static PipelineResult sucesso(final CanonicalRecord canonical,
                final PiiResult pii, final LineageRecord lineage,
                final QualityResult quality) {
            return new PipelineResult(true, canonical, pii, lineage, quality,
                    null, null, null);
        }

        /**
         * Cria um resultado de falha em uma etapa.
         *
         * @param etapa   etapa que falhou.
         * @param message mensagem de erro.
         * @return resultado de falha.
         */
        public static PipelineResult falha(final EtapaPipeline etapa, final String message) {
            return new PipelineResult(false, null, null, null, null, null, etapa, message);
        }

        /**
         * Cria um resultado de falha por validação.
         *
         * @param validation resultado de validação.
         * @return resultado de falha.
         */
        public static PipelineResult falhaValidacao(final ValidationResult validation) {
            String msg = String.join("; ", validation.getErrors());
            return new PipelineResult(false, null, null, null, null, validation,
                    EtapaPipeline.VALIDATION, msg);
        }

        /**
         * Cria um resultado de falha por qualidade.
         *
         * @param quality resultado de qualidade.
         * @return resultado de falha.
         */
        public static PipelineResult falhaQualidade(final QualityResult quality) {
            String msg = "Pontuação de qualidade insuficiente: " + quality.getScore()
                    + "/100 (mínimo: " + MIN_QUALITY_SCORE + ")";
            return new PipelineResult(false, null, null, null, quality, null,
                    EtapaPipeline.QUALITY, msg);
        }

        /**
         * Retorna se o pipeline foi bem-sucedido.
         *
         * @return sucesso.
         */
        public boolean isSucesso() {
            return sucesso;
        }

        /**
         * Retorna o registro canônico.
         *
         * @return canonicalRecord.
         */
        public CanonicalRecord getCanonicalRecord() {
            return canonicalRecord;
        }

        /**
         * Retorna o resultado PII.
         *
         * @return piiResult.
         */
        public PiiResult getPiiResult() {
            return piiResult;
        }

        /**
         * Retorna o registro de linhagem.
         *
         * @return lineageRecord.
         */
        public LineageRecord getLineageRecord() {
            return lineageRecord;
        }

        /**
         * Retorna o resultado de qualidade.
         *
         * @return qualityResult.
         */
        public QualityResult getQualityResult() {
            return qualityResult;
        }

        /**
         * Retorna o resultado de validação.
         *
         * @return validationResult.
         */
        public ValidationResult getValidationResult() {
            return validationResult;
        }

        /**
         * Retorna a etapa que falhou.
         *
         * @return etapaFalha.
         */
        public EtapaPipeline getEtapaFalha() {
            return etapaFalha;
        }

        /**
         * Retorna a mensagem de falha.
         *
         * @return mensagemFalha.
         */
        public String getMensagemFalha() {
            return mensagemFalha;
        }

        /**
         * Retorna a data de execução.
         *
         * @return executadoEm.
         */
        public LocalDateTime getExecutadoEm() {
            return executadoEm;
        }
    }

    /**
     * Etapas do pipeline.
     */
    public enum EtapaPipeline {
        /**
         * Canonicalização.
         */
        CANONICALIZATION("Canonicalização"),
        /**
         * Validação de Schema.
         */
        VALIDATION("Validação"),
        /**
         * Qualidade de Dados.
         */
        QUALITY("Qualidade"),
        /**
         * Classificação PII.
         */
        PII("PII"),
        /**
         * Linhagem.
         */
        LINEAGE("Linhagem"),
        /**
         * Publicação.
         */
        PUBLICATION("Publicação");

        private final String descricao;

        EtapaPipeline(final String desc) {
            this.descricao = desc;
        }

        /**
         * Retorna a descrição da etapa.
         *
         * @return a descrição.
         */
        public String getDescricao() {
            return descricao;
        }
    }
}
