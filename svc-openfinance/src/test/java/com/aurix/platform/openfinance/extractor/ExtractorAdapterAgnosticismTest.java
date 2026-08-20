package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.extractor.adapter.AccountSourceAdapter;
import com.aurix.platform.openfinance.extractor.dto.RawData;
import com.aurix.platform.openfinance.extractor.dto.ResourceDescriptor;
import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prova, sem subir o Spring context, que CoreAccountExtractor é agnóstico de core:
 * basta trocar a implementação de AccountSourceAdapter injetada — nenhuma linha do
 * extractor precisa mudar para "plugar" em outro core.
 */
class ExtractorAdapterAgnosticismTest {

    @Test
    void deveUsarQualquerImplementacaoDeAccountSourceAdapterSemAlterarOExtractor() {
        AccountSourceAdapter adapterDeOutroCore = new AccountSourceAdapter() {
            @Override
            public List<Account> findAccounts(String consentId) {
                return List.of(new Account("acc-outro-core", "OUTRO_BANCO", "BRL",
                        "CONTA_DE_DEPOSITO", "ACTIVE", null, null, LocalDateTime.now()));
            }

            @Override
            public List<Transaction> findTransactions(String consentId) {
                return List.of();
            }
        };

        CoreAccountExtractor extractor = new CoreAccountExtractor(adapterDeOutroCore);

        AuthorizedContext context = new AuthorizedContext(
                "ctx-agnostic", "subject-1", "consent-agnostic", 1,
                "accounts", "[\"accounts:READ\"]", "[\"resource-accounts\"]",
                LocalDateTime.now().plusHours(1), "ES256", "thumb-1", "sig-1");
        ResourceDescriptor resource = ResourceDescriptor.of("resource-accounts", ResourceType.ACCOUNTS);

        RawData raw = extractor.extract(context, resource);

        assertEquals(1, raw.getRecordCount());
        @SuppressWarnings("unchecked")
        List<AccountSourceAdapter.Account> contas =
                (List<AccountSourceAdapter.Account>) raw.getPayload().get("contas");
        assertEquals("OUTRO_BANCO", contas.get(0).institutionCode());
    }
}
