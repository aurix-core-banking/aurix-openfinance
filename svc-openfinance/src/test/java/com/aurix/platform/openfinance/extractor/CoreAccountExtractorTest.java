package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.entity.ContaConsentida;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import com.aurix.platform.openfinance.repository.ContaConsentidaRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que o extractor lê do banco de verdade (ContaConsentidaRepository) em
 * vez de retornar listas vazias fixas.
 */
@SpringBootTest
@ActiveProfiles("test")
class CoreAccountExtractorTest {

    @Autowired
    private CoreAccountExtractor extractor;

    @Autowired
    private ContaConsentidaRepository contaRepository;

    @Autowired
    private ConsentimentoRepository consentimentoRepository;

    @BeforeEach
    void setUp() {
        contaRepository.deleteAll();
        consentimentoRepository.deleteAll();
    }

    @Test
    void deveExtrairContasReaisDoConsentimento() {
        Consentimento consentimento = new Consentimento();
        consentimento.setConsentId("consent-extractor-test");
        consentimento.setClientId("client-x");
        consentimento.setUserId(1L);
        consentimento.setStatus(Consentimento.StatusConsentimento.AUTHORISED);
        consentimento.setPermissions("accounts");
        consentimento.setDataCriacao(LocalDateTime.now());
        consentimento.setDataExpiracao(LocalDateTime.now().plusDays(30));
        consentimentoRepository.save(consentimento);

        ContaConsentida conta = new ContaConsentida();
        conta.setConsentId("consent-extractor-test");
        conta.setAccountId("acc-1");
        conta.setInstitutionCode("AURIX");
        conta.setMoeda("BRL");
        conta.setTipoConta(ContaConsentida.TipoConta.CONTA_DE_DEPOSITO);
        conta.setStatusConta("ACTIVE");
        conta.setDataAtualizacao(LocalDateTime.now());
        contaRepository.save(conta);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-1", "subject-1", "consent-extractor-test", 1,
                "accounts", "[\"accounts:READ\"]", "[\"resource-accounts\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-1", "sig-1");

        ResourceDescriptor resource = ResourceDescriptor.of("resource-accounts", ResourceType.ACCOUNTS);

        RawData raw = extractor.extract(context, resource);

        assertEquals(1, raw.getRecordCount());
        assertTrue(((java.util.List<?>) raw.getPayload().get("contas")).stream()
                .anyMatch(c -> c instanceof com.aurix.platform.openfinance.extractor.adapter.AccountSourceAdapter.Account a
                        && "acc-1".equals(a.accountId())));
    }
}
