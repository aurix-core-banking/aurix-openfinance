package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.PessoaConsentida;
import com.aurix.platform.openfinance.extractor.adapter.CustomerSourceAdapter;
import com.aurix.platform.openfinance.repository.PessoaConsentidaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação do {@link CustomerSourceAdapter} para o core "aurix".
 */
@Component
@org.springframework.context.annotation.Primary
public class AurixCoreCustomerSourceAdapter implements CustomerSourceAdapter {

    private final PessoaConsentidaRepository repository;

    public AurixCoreCustomerSourceAdapter(PessoaConsentidaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CustomerRecord> findCustomers(String consentId) {
        return repository.findByConsentId(consentId).stream()
                .map(this::toRecord)
                .toList();
    }

    private CustomerRecord toRecord(PessoaConsentida p) {
        String personType = p.getTipoPessoa() == PessoaConsentida.TipoPessoa.JURIDICA ? "PJ" : "PF";
        return new CustomerRecord(
                p.getCustomerId(),
                p.getCpfCnpj(),
                personType,
                p.getNomeCompleto(),
                p.getEmail(),
                p.getTelefone(),
                "ATIVO",
                p.getDataAtualizacao()
        );
    }
}
