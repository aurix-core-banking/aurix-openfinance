package com.aurix.platform.openfinance.extractor.adapter.aurixcore;

import com.aurix.platform.openfinance.entity.PixConsentido;
import com.aurix.platform.openfinance.extractor.adapter.PixSourceAdapter;
import com.aurix.platform.openfinance.repository.PixConsentidoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação do {@link PixSourceAdapter} para o core banking "aurix".
 */
@Component
public class AurixCorePixSourceAdapter implements PixSourceAdapter {

    private final PixConsentidoRepository pixRepository;

    public AurixCorePixSourceAdapter(PixConsentidoRepository pixRepository) {
        this.pixRepository = pixRepository;
    }

    @Override
    public List<PixTransaction> findPixTransactions(String consentId) {
        return pixRepository.findByConsentId(consentId).stream().map(this::toPixTransaction).toList();
    }

    private PixTransaction toPixTransaction(PixConsentido p) {
        return new PixTransaction(
                p.getPixId(),
                p.getClienteId(),
                p.getTipoPix(),
                p.getChavedePix(),
                p.getTipoChave(),
                p.getValor(),
                p.getMoeda(),
                p.getDescricao(),
                p.getStatusPix(),
                p.getDataPix(),
                p.getDataAtualizacao());
    }
}
