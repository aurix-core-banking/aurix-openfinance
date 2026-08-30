package com.aurix.platform.openfinance.trust.receipt.service;

import com.aurix.platform.openfinance.trust.receipt.entity.ConsentReceipt;
import com.aurix.platform.openfinance.trust.receipt.repository.ConsentReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica a formalização do consentimento — recibo assinado criptograficamente
 * (não-repúdio), imutável e verificável (issue #1 / WP2).
 */
@SpringBootTest
@ActiveProfiles("test")
class ConsentReceiptServiceTest {

    @Autowired
    private ConsentReceiptService service;

    @Autowired
    private ConsentReceiptRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void deveGerarReciboAssinado() {
        ConsentReceipt receipt = service.gerarRecibo(
                "consent-1", "client-1", "accounts,credit-cards",
                "dados", LocalDateTime.now().plusDays(30));

        assertNotNull(receipt.getReceiptId());
        assertEquals("consent-1", receipt.getConsentId());
        assertNotNull(receipt.getContentHash());
        assertNotNull(receipt.getSignature());
        assertEquals("ES256", receipt.getSigningAlgorithm());
    }

    @Test
    void deveVerificarReciboCriptograficamente() {
        service.gerarRecibo("consent-2", "client-2", "accounts",
                "dados", LocalDateTime.now().plusDays(30));

        assertTrue(service.verificarRecibo("consent-2"));
    }

    @Test
    void reciboInexistenteNaoVerifica() {
        assertFalse(service.verificarRecibo("consent-inexistente"));
    }

    @Test
    void deveSerIdempotentePorConsentimento() {
        service.gerarRecibo("consent-3", "client-3", "accounts",
                "dados", LocalDateTime.now().plusDays(30));
        ConsentReceipt segundo = service.gerarRecibo("consent-3", "client-3", "accounts",
                "dados", LocalDateTime.now().plusDays(30));

        // Só um recibo por consentimento — o segundo retorna o existente
        assertEquals(1, repository.count());
        ConsentReceipt doBanco = repository.findByConsentId("consent-3").orElseThrow();
        assertEquals(doBanco.getReceiptId(), segundo.getReceiptId());
    }
}
