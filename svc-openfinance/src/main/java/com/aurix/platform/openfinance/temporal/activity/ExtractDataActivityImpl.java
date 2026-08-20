package com.aurix.platform.openfinance.temporal.activity;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.repository.AuthorizedContextRepository;
import com.aurix.platform.openfinance.extractor.DataExtractor;
import com.aurix.platform.openfinance.extractor.ExtractorRegistry;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação da atividade de extração de dados.
 * Resolve o AuthorizedContext do consentimento, encontra o extractor certo via
 * ExtractorRegistry (que por sua vez é agnóstico de core — ver extractor/adapter/)
 * e delega a extração de verdade. NÃO toma decisões de autorização: quem decide
 * isso é o Policy Engine, antes do plano de execução existir (INV04); esta
 * activity só recusa o contexto se ele não estiver ativo/não existir.
 */
@Component
public class ExtractDataActivityImpl implements ExtractDataActivity {

    private static final Logger log = LoggerFactory.getLogger(ExtractDataActivityImpl.class);

    private static final Map<String, ResourceType> RESOURCE_TYPE_BY_DOMAIN = Map.ofEntries(
            Map.entry("contas", ResourceType.ACCOUNTS),
            Map.entry("transacoes", ResourceType.TRANSACTIONS),
            Map.entry("cartoes", ResourceType.CREDIT_CARDS),
            Map.entry("faturas", ResourceType.CARD_BILLS),
            Map.entry("transacoes_cartao", ResourceType.CARD_TRANSACTIONS),
            Map.entry("emprestimos", ResourceType.LOANS),
            Map.entry("pix", ResourceType.PIX_TRANSACTIONS),
            Map.entry("investimentos", ResourceType.INVESTMENTS)
    );

    private final AuthorizedContextRepository authorizedContextRepository;
    private final ExtractorRegistry extractorRegistry;
    private final ObjectMapper objectMapper;

    public ExtractDataActivityImpl(AuthorizedContextRepository authorizedContextRepository,
                                    ExtractorRegistry extractorRegistry,
                                    ObjectMapper objectMapper) {
        this.authorizedContextRepository = authorizedContextRepository;
        this.extractorRegistry = extractorRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExtractResult extract(ExtractRequest request) {
        String activityId = Activity.getExecutionContext().getInfo().getActivityId();
        log.info("Iniciando extração - node: {}, recurso: {}, activity: {}",
                request.getNodeId(), request.getResource(), activityId);

        long startTime = System.currentTimeMillis();
        ExtractResult result = new ExtractResult();
        result.setNodeId(request.getNodeId());

        try {
            ResourceType type = RESOURCE_TYPE_BY_DOMAIN.get(request.getResource());
            if (type == null) {
                throw new IllegalArgumentException("Domínio de recurso sem extractor: " + request.getResource());
            }

            List<AuthorizedContext> activeContexts =
                    authorizedContextRepository.findActiveContexts(request.getConsentId());
            if (activeContexts.isEmpty()) {
                throw new IllegalStateException(
                        "Nenhum AuthorizedContext ativo para consentId: " + request.getConsentId());
            }
            AuthorizedContext context = activeContexts.get(0);

            Optional<DataExtractor> extractor = extractorRegistry.getExtractor(type);
            if (extractor.isEmpty()) {
                throw new IllegalStateException("Nenhum extractor registrado para tipo: " + type);
            }

            RawData rawData = extractor.get().extract(context,
                    ResourceDescriptor.of(request.getResource(), type));

            result.setRecordCount(rawData.getRecordCount());
            result.setRawData(objectMapper.writeValueAsString(rawData.getPayload()));
            result.setSuccess(true);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Extração concluída - node: {}, registros: {}, duração: {}ms",
                    request.getNodeId(), rawData.getRecordCount(), duration);

            return result;
        } catch (Exception e) {
            log.error("Falha na extração - node: {}, erro: {}", request.getNodeId(), e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
}
