package com.aurix.platform.openfinance.discovery.service;

import com.aurix.platform.openfinance.discovery.entity.ResourceEdge;
import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import com.aurix.platform.openfinance.discovery.entity.ResourceNode;
import com.aurix.platform.openfinance.discovery.repository.ResourceEdgeRepository;
import com.aurix.platform.openfinance.discovery.repository.ResourceGraphRepository;
import com.aurix.platform.openfinance.discovery.repository.ResourceNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de Descoberta de Recursos — constrói o grafo autorizado de recursos
 * a partir de permissões do consentimento. Gera DAG de dependências de extração.
 */
@Service
public class ResourceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ResourceDiscoveryService.class);

    private final ResourceGraphRepository graphRepository;
    private final ResourceNodeRepository nodeRepository;
    private final ResourceEdgeRepository edgeRepository;

    public ResourceDiscoveryService(ResourceGraphRepository graphRepository,
                                    ResourceNodeRepository nodeRepository,
                                    ResourceEdgeRepository edgeRepository) {
        this.graphRepository = graphRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    /**
     * Descobre e constrói o grafo de recursos autorizados a partir do consentimento.
     * Gera um DAG de dependências de extração.
     */
    @Transactional
    public ResourceGraph discover(String consentId, List<String> permissions,
                                  Map<String, String> resourceMetadata) {
        String graphId = UUID.randomUUID().toString();
        List<ResourceNode> nodes = new ArrayList<>();
        List<ResourceEdge> edges = new ArrayList<>();

        Map<String, ResourceNode> nodeIndex = new HashMap<>();

        for (String permission : permissions) {
            String resourceId = graphId + ":" + permission;
            String path = resolveResourcePath(permission);

            ResourceNode node = new ResourceNode(
                    resourceId,
                    graphId,
                    extractResourceType(permission),
                    path,
                    buildCapabilities(permission),
                    "[]",
                    resourceMetadata != null ? resourceMetadata.getOrDefault(permission, "{}") : "{}"
            );

            nodes.add(node);
            nodeIndex.put(permission, node);
        }

        for (ResourceNode source : nodes) {
            for (ResourceNode target : nodes) {
                if (source.equals(target)) {
                    continue;
                }
                if (hasDependency(source, target)) {
                    ResourceEdge edge = new ResourceEdge(
                            graphId,
                            source.getNodeId(),
                            target.getNodeId(),
                            "DEPENDS_ON",
                            "{}"
                    );
                    edges.add(edge);
                }
            }
        }

        List<ResourceEdge> cycleEdges = detectCycles(graphId, edges);
        if (!cycleEdges.isEmpty()) {
            log.warn("Ciclos detectados no grafo {} — removendo {} arestas",
                    graphId, cycleEdges.size());
            edges.removeAll(cycleEdges);
        }

        ResourceGraph graph = new ResourceGraph(graphId, consentId, 1);
        graph.setNodes(nodes);
        graph.setEdges(edges);

        graphRepository.save(graph);
        nodeRepository.saveAll(nodes);
        edgeRepository.saveAll(edges);

        log.info("Grafo de recursos descoberto: graphId={}, consentId={}, nós={}, arestas={}",
                graphId, consentId, nodes.size(), edges.size());

        return graph;
    }

    /**
     * Obtém o grafo de recursos para um consentimento.
     */
    @Transactional(readOnly = true)
    public ResourceGraph getByConsentId(String consentId) {
        return graphRepository.findLatestByConsentId(consentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Grafo não encontrado para consent: " + consentId));
    }

    /**
     * Obtém grafo por ID.
     */
    @Transactional(readOnly = true)
    public ResourceGraph getByGraphId(String graphId) {
        return graphRepository.findByGraphId(graphId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Grafo não encontrado: " + graphId));
    }

    /**
     * Retorna nós folha (sem dependências) — pontos de extração possíveis.
     */
    @Transactional(readOnly = true)
    public List<ResourceNode> getLeafNodes(String graphId) {
        return nodeRepository.findLeafNodes(graphId);
    }

    /**
     * Gera ordenação topológica do grafo (para extração sequencial).
     */
    @Transactional(readOnly = true)
    public List<String> topologicalSort(String graphId) {
        List<ResourceNode> nodes = nodeRepository.findByGraphIdAndActiveTrue(graphId);
        List<ResourceEdge> edges = edgeRepository.findByGraphId(graphId);

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (ResourceNode node : nodes) {
            inDegree.put(node.getNodeId(), 0);
            adjacency.put(node.getNodeId(), new ArrayList<>());
        }

        for (ResourceEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>())
                    .add(edge.getTargetNodeId());
            inDegree.merge(edge.getTargetNodeId(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(current);
            for (String neighbor : adjacency.getOrDefault(current, List.of())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return sorted;
    }

    private String resolveResourcePath(String permission) {
        return "/openfinance/v1/resources/" + permission.toLowerCase().replace("_", "/");
    }

    private String extractResourceType(String permission) {
        String[] parts = permission.split("_");
        return parts.length > 0 ? parts[0].toLowerCase() : "unknown";
    }

    private String buildCapabilities(String permission) {
        return "[\"" + permission.toLowerCase() + "\"]";
    }

    private boolean hasDependency(ResourceNode source, ResourceNode target) {
        String sourceType = source.getResourceType();
        String targetType = target.getResourceType();

        Map<String, List<String>> dependencyRules = Map.of(
                "accounts", List.of("balances", "transactions"),
                "credit", List.of("accounts"),
                "loans", List.of("accounts"),
                "cards", List.of("accounts", "balances")
        );

        return dependencyRules.getOrDefault(sourceType, List.of()).contains(targetType);
    }

    private List<ResourceEdge> detectCycles(String graphId, List<ResourceEdge> edges) {
        List<ResourceEdge> toRemove = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();

        Map<String, List<String>> adjacency = new HashMap<>();
        for (ResourceEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>())
                    .add(edge.getTargetNodeId());
        }

        for (ResourceEdge edge : edges) {
            if (hasCycle(edge.getSourceNodeId(), adjacency, visited, inStack)) {
                toRemove.add(edge);
            }
        }

        return toRemove;
    }

    private boolean hasCycle(String nodeId, Map<String, List<String>> adjacency,
                             Set<String> visited, Set<String> inStack) {
        if (inStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        inStack.add(nodeId);

        for (String neighbor : adjacency.getOrDefault(nodeId, List.of())) {
            if (hasCycle(neighbor, adjacency, visited, inStack)) {
                return true;
            }
        }

        inStack.remove(nodeId);
        return false;
    }
}
