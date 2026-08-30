package com.aurix.platform.openfinance.trust.receipt.service;

import com.aurix.platform.openfinance.trust.receipt.entity.ConsentReceipt;
import com.aurix.platform.openfinance.trust.receipt.repository.ConsentReceiptRepository;
import com.aurix.platform.openfinance.trust.service.SigningService;
import com.aurix.platform.openfinance.trust.service.SignedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de recibo de consentimento — assina formalmente o momento e o
 * conteúdo do consentimento (não-repúdio), usando a {@link SigningService} real.
 */
@Service
public class ConsentReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ConsentReceiptService.class);

    private final ConsentReceiptRepository receiptRepository;
    private final SigningService signingService;
    private final String signingAlgorithm;

    public ConsentReceiptService(
            ConsentReceiptRepository receiptRepository,
            SigningService signingService,
            @Value("${aurix.openfinance.trust.receipt.algorithm:ES256}") String signingAlgorithm) {
        this.receiptRepository = receiptRepository;
        this.signingService = signingService;
        this.signingAlgorithm = signingAlgorithm;
    }

    /**
     * Gera e persiste o recibo de consentimento (imutável) para um consentimento
     * recém-aprovado. O conteúdo assinado é o hash do que foi consentido
     * (permissions + purpose + validade).
     *
     * @param consentId     identificador do consentimento
     * @param clientId      cliente que consentiu
     * @param permissions   permissões concedidas
     * @param purpose       finalidade
     * @param validUntil    validade
     * @return o recibo persistido, ou o existente se já gerado (idempotente)
     */
    @Transactional
    public ConsentReceipt gerarRecibo(String consentId, String clientId,
                                      String permissions, String purpose,
                                      LocalDateTime validUntil) {
        Optional<ConsentReceipt> existing = receiptRepository.findByConsentId(consentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String content = "consentId=" + consentId
                + "|clientId=" + clientId
                + "|permissions=" + permissions
                + "|purpose=" + (purpose == null ? "" : purpose)
                + "|validUntil=" + validUntil;
        String contentHash = sha256Hex(content);

        // Trunca para segundos para garantir consistência com a coluna TIMESTAMP
        // do banco (que pode perder precisão de nanosegundos na leitura).
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String payload = consentId + "|" + contentHash
                + "|" + now;
        SignedPayload signed = signingService.sign(payload, signingAlgorithm);

        ConsentReceipt receipt = new ConsentReceipt(
                UUID.randomUUID().toString(),
                consentId,
                contentHash,
                now,
                signed.signature(),
                signed.algorithm()
        );

        ConsentReceipt saved = receiptRepository.save(receipt);
        log.info("Recibo de consentimento gerado: consentId={}", consentId);
        return saved;
    }

    /**
     * Verifica se um recibo é criptograficamente válido (não-repúdio).
     */
    @Transactional(readOnly = true)
    public boolean verificarRecibo(String consentId) {
        return receiptRepository.findByConsentId(consentId)
                .map(r -> {
                    String payload = r.getConsentId() + "|" + r.getContentHash()
                            + "|" + r.getTimestamp().withNano(0);
                    return signingService.verify(payload, r.getSignature(), r.getSigningAlgorithm());
                })
                .orElse(false);
    }

    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao computar hash SHA-256", e);
        }
    }
}
