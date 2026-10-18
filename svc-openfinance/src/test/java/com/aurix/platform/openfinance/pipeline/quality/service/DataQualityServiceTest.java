package com.aurix.platform.openfinance.pipeline.quality.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.quality.entity.QualityResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários para avaliação de regras de qualidade em {@link DataQualityService}.
 */
class DataQualityServiceTest {

    private DataQualityService service;

    @BeforeEach
    void setUp() {
        service = new DataQualityService();
    }

    @Test
    @DisplayName("Deve aprovar registro canônico de CONTA íntegro com pontuação 100")
    void deveAprovarRegistroIntegroComPontuacaoMaxima() throws Exception {
        String json = "{\"accountId\":\"acc-1\",\"accountType\":\"CORRENTE\"}";
        CanonicalRecord record = criarRecord(ResourceType.CONTA, json, sha256(json));

        QualityResult result = service.checkQuality(record, ResourceType.CONTA);

        assertTrue(result.isPassed());
        assertEquals(100, result.getScore());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("Deve detectar divergência de checksum SHA-256 na regra ACUR002")
    void deveDetectarDivergenciaDeChecksumAcur002() {
        String json = "{\"accountId\":\"acc-1\",\"accountType\":\"CORRENTE\"}";
        String checksumFalso = "0".repeat(64);
        CanonicalRecord record = criarRecord(ResourceType.CONTA, json, checksumFalso);

        QualityResult result = service.checkQuality(record, ResourceType.CONTA);

        assertTrue(result.getIssues().stream().anyMatch(i -> "ACUR002".equals(i.getRuleId())),
                "Deveria apontar violação na regra ACUR002 quando o checksum não corresponde ao payload");
    }

    @Test
    @DisplayName("Deve avaliar regras específicas de domínio (CONTA001, TRAN001, CART001, PIX001)")
    void deveAvaliarRegrasEspecificasDeDominio() throws Exception {
        String jsonContaInvalida = "{\"accountId\":\"acc-1\",\"accountType\":\"CRIPTO\"}";
        QualityResult resConta = service.checkQuality(
                criarRecord(ResourceType.CONTA, jsonContaInvalida, sha256(jsonContaInvalida)),
                ResourceType.CONTA);
        assertTrue(resConta.getIssues().stream().anyMatch(i -> "CONTA001".equals(i.getRuleId())),
                "Deveria apontar violação em CONTA001 para tipo de conta inválido");

        String jsonTransacaoInvalida = "{\"transactionId\":\"tx-1\",\"type\":\"ESTORNO_INVALIDO\"}";
        QualityResult resTran = service.checkQuality(
                criarRecord(ResourceType.TRANSACAO, jsonTransacaoInvalida, sha256(jsonTransacaoInvalida)),
                ResourceType.TRANSACAO);
        assertTrue(resTran.getIssues().stream().anyMatch(i -> "TRAN001".equals(i.getRuleId())),
                "Deveria apontar violação em TRAN001 para tipo de transação inválido");

        String jsonCartaoSemBandeira = "{\"cardId\":\"card-1\",\"brand\":null}";
        QualityResult resCartao = service.checkQuality(
                criarRecord(ResourceType.CARTAO, jsonCartaoSemBandeira, sha256(jsonCartaoSemBandeira)),
                ResourceType.CARTAO);
        assertTrue(resCartao.getIssues().stream().anyMatch(i -> "CART001".equals(i.getRuleId())),
                "Deveria apontar violação em CART001 quando bandeira estiver ausente");

        String jsonPixSemEndToEnd = "{\"pixId\":\"pix-1\",\"endToEndId\":\"\"}";
        QualityResult resPix = service.checkQuality(
                criarRecord(ResourceType.PIX, jsonPixSemEndToEnd, sha256(jsonPixSemEndToEnd)),
                ResourceType.PIX);
        assertFalse(resPix.getIssues().stream().filter(i -> "PIX001".equals(i.getRuleId())).toList().isEmpty(),
                "Deveria apontar violação em PIX001 quando endToEndId/chavePix estiver inválido");
    }

    private CanonicalRecord criarRecord(final ResourceType type, final String json, final String checksum) {
        CanonicalRecord record = new CanonicalRecord();
        record.setCanonicalId("can-1");
        record.setRawRecordId("raw-1");
        record.setResourceType(type);
        record.setCanonicalData(json);
        record.setVersion("1.0");
        record.setCanonicalizedAt(LocalDateTime.now());
        record.setChecksum(checksum);
        return record;
    }

    private String sha256(final String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
