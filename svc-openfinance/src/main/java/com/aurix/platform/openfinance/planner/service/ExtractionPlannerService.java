package com.aurix.platform.openfinance.planner.service;

import com.aurix.platform.openfinance.planner.dto.PlanRequest;
import com.aurix.platform.openfinance.planner.dto.PlanResponse;
import com.aurix.platform.openfinance.planner.entity.ExecutionPlan;
import com.aurix.platform.openfinance.planner.entity.PlanNode;
import com.aurix.platform.openfinance.planner.entity.PlanStatus;
import com.aurix.platform.openfinance.planner.repository.ExecutionPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de criação e validação de planos de extração.
 * Gera um DAG imutável a partir do grafo de recursos autorizados.
 */
@Service
@Transactional
public class ExtractionPlannerService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionPlannerService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, List<String>> RESOURCE_DEPENDENCIES = Map.of(
            "contas", List.of(),
            "transacoes", List.of("contas"),
            "cartoes", List.of("contas"),
            "faturas", List.of("cartoes"),
            "transacoes_cartao", List.of("cartoes"),
            "emprestimos", List.of(),
            "seguros", List.of(),
            "pix", List.of("contas"),
            "pessoas", List.of()
    );

    private static final Map<String, Integer> RESOURCE_TIMEOUTS = Map.of(
            "contas", 30,
            "transacoes", 120,
            "cartoes", 30,
            "faturas", 60,
            "transacoes_cartao", 120,
            "emprestimos", 30,
            "seguros", 30,
            "pix", 30,
            "pessoas", 15
    );

    private final ExecutionPlanRepository planRepository;

    public ExtractionPlannerService(ExecutionPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Cria um plano de execução a partir do contexto autorizado e dos recursos solicitados.
     */
    public PlanResponse criarPlano(PlanRequest request) {
        log.info("Criando plano de extração para consentimento: {}", request.getConsentId());

        List<String> recursos = request.getRequestedResources();
        if (recursos == null || recursos.isEmpty()) {
            recursos = RESOURCE_DEPENDENCIES.keySet().stream().toList();
        }

        List<PlanNode> nodes = construirNodes(recursos, request);
        List<PlanResponse.EdgeInfo> edges = construirEdges(nodes);
        List<String> order = ordenacaoTopologica(nodes);

        validarLayoutDag(nodes, edges, order);

        String dagJson = serializarDag(nodes, order);
        String metadataJson = serializarMetadata(request);

        ExecutionPlan plano = new ExecutionPlan();
        plano.setPlanId(gerarPlanId());
        plano.setConsentId(request.getConsentId());
        plano.setConsentVersion(request.getConsentVersion());
        plano.setParticipanteId(request.getParticipantId());
        plano.setDagDefinition(dagJson);
        plano.setMetadata(metadataJson);
        plano.setStatus(PlanStatus.CRIADO);
        plano.setDataCriacao(LocalDateTime.now());

        ExecutionPlan planoSalvo = planRepository.save(plano);
        log.info("Plano criado com sucesso: {}", planoSalvo.getPlanId());

        return converterParaResponse(planoSalvo, nodes, edges);
    }

    /**
     * Valida se o DAG é acíclico, sem nós órfãos, e todas dependências existem.
     */
    @Transactional(readOnly = true)
    public boolean validarDag(String dagJson) {
        try {
            Map<String, Object> dag = objectMapper.readValue(dagJson, new TypeReference<>() {});
            List<Map<String, Object>> nodesList = (List<Map<String, Object>>) dag.get("nodes");
            List<Map<String, Object>> edgesList = (List<Map<String, Object>>) dag.get("edges");

            Set<String> nodeIds = nodesList.stream()
                    .map(n -> (String) n.get("node_id"))
                    .collect(Collectors.toSet());

            Map<String, Set<String>> adjacencia = new HashMap<>();
            for (String nodeId : nodeIds) {
                adjacencia.put(nodeId, new HashSet<>());
            }

            for (Map<String, Object> edge : edgesList) {
                String src = (String) edge.get("origem");
                String tgt = (String) edge.get("destino");
                if (!nodeIds.contains(src) || !nodeIds.contains(tgt)) {
                    log.warn("Aresta referencia node inexistente: {} -> {}", src, tgt);
                    return false;
                }
                adjacencia.get(src).add(tgt);
            }

            return !temCiclo(adjacencia, nodeIds);
        } catch (JsonProcessingException e) {
            log.error("Erro ao parsear DAG JSON: {}", e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Optional<ExecutionPlan> buscarPorPlanId(String planId) {
        return planRepository.findByPlanId(planId);
    }

    @Transactional(readOnly = true)
    public Optional<ExecutionPlan> buscarPorId(Long id) {
        return planRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ExecutionPlan> listarPorConsentimento(String consentId) {
        return planRepository.findByConsentId(consentId);
    }

    public ExecutionPlan cancelarPlano(String planId, String motivo) {
        ExecutionPlan plano = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + planId));

        if (plano.getStatus() == PlanStatus.CONCLUIDO || plano.getStatus() == PlanStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível cancelar plano em status: " + plano.getStatus());
        }

        plano.setStatus(PlanStatus.CANCELADO);
        return planRepository.save(plano);
    }

    public void atualizarStatus(String planId, PlanStatus novoStatus) {
        ExecutionPlan plano = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + planId));
        plano.setStatus(novoStatus);
        if (novoStatus == PlanStatus.EM_EXECUCAO) {
            plano.setDataExecucao(LocalDateTime.now());
        } else if (novoStatus == PlanStatus.CONCLUIDO || novoStatus == PlanStatus.FALHADO) {
            plano.setDataConclusao(LocalDateTime.now());
        }
        planRepository.save(plano);
    }

    private List<PlanNode> construirNodes(List<String> recursos, PlanRequest request) {
        List<PlanNode> nodes = new ArrayList<>();
        Set<String> adicionados = new HashSet<>();

        for (String recurso : recursos) {
            adicionarNodeEDependencias(recurso, recursos, nodes, adicionados, request);
        }

        return nodes;
    }

    private void adicionarNodeEDependencias(String recurso, List<String> recursosSolicitados,
                                            List<PlanNode> nodes, Set<String> adicionados,
                                            PlanRequest request) {
        if (adicionados.contains(recurso)) {
            return;
        }

        List<String> deps = RESOURCE_DEPENDENCIES.getOrDefault(recurso, List.of());
        for (String dep : deps) {
            adicionarNodeEDependencias(dep, recursosSolicitados, nodes, adicionados, request);
        }

        List<String> dependenciasFiltradas = deps.stream()
                .filter(recursosSolicitados::contains)
                .toList();

        PlanNode node = new PlanNode();
        node.setNodeId("node_" + recurso);
        node.setCapability(recurso);
        node.setResource(recurso);
        node.setDependencies(dependenciasFiltradas.stream().map(d -> "node_" + d).toList());
        node.setTimeoutSeconds(RESOURCE_TIMEOUTS.getOrDefault(recurso, 30));
        node.setIdempotencyKey(UUID.randomUUID().toString());

        PlanNode.RetryPolicy retryPolicy = new PlanNode.RetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryPolicy.setIntervalMs(1000);
        retryPolicy.setExponentialBackoff(true);
        node.setRetryPolicy(retryPolicy);

        PlanNode.RateLimitInfo rateLimit = new PlanNode.RateLimitInfo();
        rateLimit.setRequestsPerSecond(10);
        rateLimit.setRequestsPerMinute(200);
        node.setRateLimit(rateLimit);

        node.setSchemaVersion("1.0.0");

        nodes.add(node);
        adicionados.add(recurso);
    }

    private List<PlanResponse.EdgeInfo> construirEdges(List<PlanNode> nodes) {
        List<PlanResponse.EdgeInfo> edges = new ArrayList<>();
        for (PlanNode node : nodes) {
            if (node.getDependencies() != null) {
                for (String dep : node.getDependencies()) {
                    PlanResponse.EdgeInfo edge = new PlanResponse.EdgeInfo();
                    edge.setSource(dep);
                    edge.setTarget(node.getNodeId());
                    edges.add(edge);
                }
            }
        }
        return edges;
    }

    /**
     * Ordenação topológica via Kahn's algorithm.
     */
    private List<String> ordenacaoTopologica(List<PlanNode> nodes) {
        Map<String, Integer> grauEntrada = new HashMap<>();
        Map<String, List<String>> adjacencia = new HashMap<>();

        for (PlanNode node : nodes) {
            grauEntrada.putIfAbsent(node.getNodeId(), 0);
            adjacencia.putIfAbsent(node.getNodeId(), new ArrayList<>());
        }

        for (PlanNode node : nodes) {
            if (node.getDependencies() != null) {
                for (String dep : node.getDependencies()) {
                    adjacencia.computeIfAbsent(dep, k -> new ArrayList<>()).add(node.getNodeId());
                    grauEntrada.merge(node.getNodeId(), 1, Integer::sum);
                }
            }
        }

        Queue<String> fila = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : grauEntrada.entrySet()) {
            if (entry.getValue() == 0) {
                fila.add(entry.getKey());
            }
        }

        List<String> ordenados = new ArrayList<>();
        while (!fila.isEmpty()) {
            String atual = fila.poll();
            ordenados.add(atual);
            for (String vizinho : adjacencia.getOrDefault(atual, List.of())) {
                int novoGrau = grauEntrada.get(vizinho) - 1;
                grauEntrada.put(vizinho, novoGrau);
                if (novoGrau == 0) {
                    fila.add(vizinho);
                }
            }
        }

        return ordenados;
    }

    private void validarLayoutDag(List<PlanNode> nodes, List<PlanResponse.EdgeInfo> edges, List<String> order) {
        if (order.size() != nodes.size()) {
            throw new IllegalArgumentException("Ciclo detectado no grafo de dependências");
        }

        Set<String> nodeIds = nodes.stream().map(PlanNode::getNodeId).collect(Collectors.toSet());
        Set<String> allDeps = nodes.stream()
                .filter(n -> n.getDependencies() != null)
                .flatMap(n -> n.getDependencies().stream())
                .collect(Collectors.toSet());

        Set<String> orfaos = new HashSet<>(allDeps);
        orfaos.removeAll(nodeIds);

        if (!orfaos.isEmpty()) {
            throw new IllegalArgumentException("Dependências referenciam nodes inexistentes: " + orfaos);
        }
    }

    private boolean temCiclo(Map<String, Set<String>> adjacencia, Set<String> nodeIds) {
        Set<String> visitados = new HashSet<>();
        Set<String> emPilha = new HashSet<>();

        for (String nodeId : nodeIds) {
            if (dfsTemCiclo(nodeId, adjacencia, visitados, emPilha)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsTemCiclo(String node, Map<String, Set<String>> adjacencia,
                                Set<String> visitados, Set<String> emPilha) {
        if (emPilha.contains(node)) {
            return true;
        }
        if (visitados.contains(node)) {
            return false;
        }

        visitados.add(node);
        emPilha.add(node);

        for (String vizinho : adjacencia.getOrDefault(node, Set.of())) {
            if (dfsTemCiclo(vizinho, adjacencia, visitados, emPilha)) {
                return true;
            }
        }

        emPilha.remove(node);
        return false;
    }

    private String serializarDag(List<PlanNode> nodes, List<String> order) {
        try {
            Map<String, Object> dag = new LinkedHashMap<>();
            dag.put("nodes", nodes);
            dag.put("execution_order", order);
            return objectMapper.writeValueAsString(dag);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar DAG: " + e.getMessage(), e);
        }
    }

    private String serializarMetadata(PlanRequest request) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("data_criacao", LocalDateTime.now().toString());
            metadata.put("valido_ate", request.getValidUntil());
            metadata.put("participante_id", request.getParticipantId());
            metadata.put("configuracoes", request.getConfiguration());
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar metadata: " + e.getMessage(), e);
        }
    }

    private String gerarPlanId() {
        return "plan-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private PlanResponse converterParaResponse(ExecutionPlan plano, List<PlanNode> nodes,
                                                List<PlanResponse.EdgeInfo> edges) {
        PlanResponse response = new PlanResponse();
        response.setPlanId(plano.getPlanId());
        response.setConsentId(plano.getConsentId());
        response.setConsentVersion(plano.getConsentVersion());
        response.setParticipantId(plano.getParticipanteId());
        response.setStatus(plano.getStatus().name());
        response.setCreatedAt(plano.getDataCriacao());
        response.setExecutedAt(plano.getDataExecucao());
        response.setCompletedAt(plano.getDataConclusao());

        List<PlanResponse.PlanNodeInfo> nodeInfos = nodes.stream().map(node -> {
            PlanResponse.PlanNodeInfo info = new PlanResponse.PlanNodeInfo();
            info.setNodeId(node.getNodeId());
            info.setCapability(node.getCapability());
            info.setResource(node.getResource());
            info.setDependencies(node.getDependencies());
            info.setTimeoutSeconds(node.getTimeoutSeconds());
            info.setIdempotencyKey(node.getIdempotencyKey());
            return info;
        }).toList();

        response.setNodes(nodeInfos);
        response.setEdges(edges);

        return response;
    }
}
