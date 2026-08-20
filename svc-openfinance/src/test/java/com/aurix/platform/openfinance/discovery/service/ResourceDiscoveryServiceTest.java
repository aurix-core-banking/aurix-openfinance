package com.aurix.platform.openfinance.discovery.service;

import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import com.aurix.platform.openfinance.discovery.repository.ResourceEdgeRepository;
import com.aurix.platform.openfinance.discovery.repository.ResourceGraphRepository;
import com.aurix.platform.openfinance.discovery.repository.ResourceNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que ResourceDiscoveryService constrói o grafo de dependências corretamente
 * (accounts precisa vir antes de balances/transactions na extração) e que a
 * ordenação topológica respeita isso.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResourceDiscoveryServiceTest {

    @Autowired
    private ResourceDiscoveryService service;

    @Autowired
    private ResourceGraphRepository graphRepository;

    @Autowired
    private ResourceNodeRepository nodeRepository;

    @Autowired
    private ResourceEdgeRepository edgeRepository;

    @BeforeEach
    void setUp() {
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();
        graphRepository.deleteAll();
    }

    @Test
    void deveDescobrirGrafoComArestaDeDependenciaAccountsParaBalances() {
        ResourceGraph graph = service.discover("consent-discovery-test",
                List.of("accounts", "balances", "transactions"), null);

        assertEquals(3, graph.getNodes().size());
        assertTrue(edgeRepository.findByGraphId(graph.getGraphId()).size() >= 2,
                "accounts deveria ter aresta DEPENDS_ON para balances e transactions");
    }

    @Test
    void ordenacaoTopologicaDeveColocarAccountsAntesDeBalances() {
        ResourceGraph graph = service.discover("consent-topo-test",
                List.of("accounts", "balances", "transactions"), null);

        List<String> sorted = service.topologicalSort(graph.getGraphId());

        int idxAccounts = indexOfNodeWithType(sorted, "accounts");
        int idxBalances = indexOfNodeWithType(sorted, "balances");

        assertTrue(idxAccounts < idxBalances,
                "accounts (" + idxAccounts + ") deveria vir antes de balances (" + idxBalances + ")");
    }

    private int indexOfNodeWithType(List<String> sortedNodeIds, String type) {
        for (int i = 0; i < sortedNodeIds.size(); i++) {
            if (sortedNodeIds.get(i).endsWith(":" + type)) {
                return i;
            }
        }
        throw new AssertionError("Nó do tipo " + type + " não encontrado na ordenação: " + sortedNodeIds);
    }
}
