package com.aurix.platform.openfinance.extractor.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registro e gerenciador de conectores de core bancário.
 */
@Service
public class CoreBankingConnectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingConnectorRegistry.class);

    private final Map<TipoConector, CoreBankingConnector> conectores;
    private final TipoConector tipoAtivo;

    public CoreBankingConnectorRegistry(
            List<CoreBankingConnector> listaConectores,
            @Value("${aurix.openfinance.connector.type:aurix}") String tipoConfigurado) {
        this.conectores = listaConectores.stream()
                .collect(Collectors.toMap(CoreBankingConnector::getTipo, c -> c, (a, b) -> a));

        this.tipoAtivo = parseTipo(tipoConfigurado);
        log.info("CoreBankingConnectorRegistry inicializado. Conector ativo: {} ({})",
                this.tipoAtivo, getConectorAtivo().getNome());
    }

    public CoreBankingConnector getConectorAtivo() {
        return conectores.getOrDefault(tipoAtivo, conectores.values().iterator().next());
    }

    public CoreBankingConnector getConector(TipoConector tipo) {
        return conectores.get(tipo);
    }

    public List<CoreBankingConnector> getTodosConectores() {
        return List.copyOf(conectores.values());
    }

    private TipoConector parseTipo(String tipo) {
        if (tipo == null) return TipoConector.AURIX_NATIVO;
        return switch (tipo.toLowerCase().trim()) {
            case "rest", "rest_http", "http" -> TipoConector.REST_HTTP;
            case "sandbox", "simulado", "mock" -> TipoConector.SANDBOX;
            default -> TipoConector.AURIX_NATIVO;
        };
    }
}
