package com.aurix.platform.openfinance.temporal.workflow;

import com.aurix.platform.openfinance.temporal.workflow.dto.ExecutionPlanRequest;
import com.aurix.platform.openfinance.temporal.activity.ExtractDataActivity;
import com.aurix.platform.openfinance.temporal.activity.PublishDataActivity;
import com.aurix.platform.openfinance.temporal.activity.TransformDataActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação do workflow de extração de dados.
 * Para cada node no DAG:
 *   1. Verificar se dependências foram concluídas
 *   2. Executar ExtractDataActivity
 *   3. Executar TransformDataActivity (canonicalização + validação)
 *   4. Executar PublishDataActivity
 * Suporta execução paralela de nodes independentes.
 * Trata sinais de cancelamento.
 */
public class DataExtractionWorkflowImpl implements DataExtractionWorkflow {

    private static final Logger log = LoggerFactory.getLogger(DataExtractionWorkflowImpl.class);

    private final ActivityOptions extractOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final ActivityOptions transformOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(3))
            .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .build())
            .build();

    private final ActivityOptions publishOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .build())
            .build();

    private final ExtractDataActivity extractActivity = Workflow.newActivityStub(
            ExtractDataActivity.class, extractOptions);

    private final TransformDataActivity transformActivity = Workflow.newActivityStub(
            TransformDataActivity.class, transformOptions);

    private final PublishDataActivity publishActivity = Workflow.newActivityStub(
            PublishDataActivity.class, publishOptions);

    private ExtractionStatus status = new ExtractionStatus();
    private boolean cancelSignal = false;
    private String cancelReason;

    @Override
    public ExtractionResult execute(ExecutionPlanRequest plano) {
        log.info("Iniciando workflow de extração - plano: {}", plano.getPlanId());
        long startTime = System.currentTimeMillis();

        status.setPlanId(plano.getPlanId());
        status.setRunning(true);

        Map<String, ExecutionPlanRequest.SerializableNode> nodeMap = new LinkedHashMap<>();
        Map<String, List<String>> adjacencyList = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();

        if (plano.getNodes() != null) {
            for (ExecutionPlanRequest.SerializableNode node : plano.getNodes()) {
                nodeMap.put(node.getNodeId(), node);
                adjacencyList.putIfAbsent(node.getNodeId(), new ArrayList<>());
                inDegree.putIfAbsent(node.getNodeId(), 0);
            }

            for (ExecutionPlanRequest.SerializableNode node : plano.getNodes()) {
                if (node.getDependencies() != null) {
                    for (String dep : node.getDependencies()) {
                        adjacencyList.computeIfAbsent(dep, k -> new ArrayList<>()).add(node.getNodeId());
                        inDegree.merge(node.getNodeId(), 1, Integer::sum);
                    }
                }
            }
        }

        Map<String, ExtractionResult.NodeResult> nodeResults = new ConcurrentHashMap<>();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        int totalNodes = nodeMap.size();
        int completedNodes = 0;

        Set<String> completed = Collections.synchronizedSet(new HashSet<>());
        Set<String> failed = Collections.synchronizedSet(new HashSet<>());

        while (completed.size() + failed.size() < totalNodes) {
            if (cancelSignal) {
                log.info("Sinal de cancelamento recebido para plano: {}", plano.getPlanId());
                status.setRunning(false);
                status.setCancelled(true);
                status.setCancelReason(cancelReason);
                break;
            }

            List<String> readyNodes = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                String nodeId = entry.getKey();
                int degree = entry.getValue();
                if (degree == 0 && !completed.contains(nodeId) && !failed.contains(nodeId)) {
                    readyNodes.add(nodeId);
                }
            }

            if (readyNodes.isEmpty() && completed.size() + failed.size() < totalNodes) {
                log.error("Deadlock detectado no DAG - nodes restantes com dependências não satisfeitas");
                errors.add("Deadlock detectado no DAG");
                break;
            }

            List<Promise<ExtractionResult.NodeResult>> promises = new ArrayList<>();
            List<String> executingNodes = new ArrayList<>();

            for (String nodeId : readyNodes) {
                ExecutionPlanRequest.SerializableNode node = nodeMap.get(nodeId);
                status.setCurrentNode(nodeId);
                executingNodes.add(nodeId);

                Promise<ExtractionResult.NodeResult> promise = Async.function(() -> {
                    return executarNode(plano, node);
                });
                promises.add(promise);
            }

            for (int i = 0; i < promises.size(); i++) {
                String nodeId = executingNodes.get(i);
                try {
                    ExtractionResult.NodeResult result = promises.get(i).get();
                    nodeResults.put(nodeId, result);

                    if (result.isSuccess()) {
                        completed.add(nodeId);
                        for (String dependent : adjacencyList.getOrDefault(nodeId, List.of())) {
                            inDegree.computeIfPresent(dependent, (k, v) -> v - 1);
                        }
                    } else {
                        failed.add(nodeId);
                        errors.add("Node " + nodeId + " falhou: " + result.getError());
                        propagarFalhaParaDependentes(nodeId, adjacencyList, failed, nodeResults, errors);
                    }
                } catch (Exception e) {
                    log.error("Erro ao executar node {}: {}", nodeId, e.getMessage());
                    ExtractionResult.NodeResult failResult = new ExtractionResult.NodeResult();
                    failResult.setNodeId(nodeId);
                    failResult.setSuccess(false);
                    failResult.setError(e.getMessage());
                    nodeResults.put(nodeId, failResult);
                    failed.add(nodeId);
                    errors.add("Node " + nodeId + " exceção: " + e.getMessage());
                    propagarFalhaParaDependentes(nodeId, adjacencyList, failed, nodeResults, errors);
                }

                completedNodes = completed.size() + failed.size();
                status.setProgressPercentage((double) completedNodes / totalNodes * 100);
            }
        }

        status.setRunning(false);

        ExtractionResult result = new ExtractionResult();
        result.setPlanId(plano.getPlanId());
        result.setSuccess(failed.isEmpty() && !status.isCancelled());
        result.setNodesProcessed(completed.size());
        result.setNodesFailed(failed.size());
        result.setNodeResults(nodeResults);
        result.setErrors(errors);
        result.setDurationMs(System.currentTimeMillis() - startTime);

        log.info("Workflow de extração concluído - plano: {}, sucesso: {}, duration: {}ms",
                plano.getPlanId(), result.isSuccess(), result.getDurationMs());

        return result;
    }

    private void propagarFalhaParaDependentes(String failedNodeId,
            Map<String, List<String>> adjacencyList,
            Set<String> failed,
            Map<String, ExtractionResult.NodeResult> nodeResults,
            List<String> errors) {
        Deque<String> fila = new ArrayDeque<>(adjacencyList.getOrDefault(failedNodeId, List.of()));
        while (!fila.isEmpty()) {
            String dep = fila.poll();
            if (failed.add(dep)) {
                ExtractionResult.NodeResult depFail = new ExtractionResult.NodeResult();
                depFail.setNodeId(dep);
                depFail.setSuccess(false);
                depFail.setError("Dependência " + failedNodeId + " falhou");
                nodeResults.put(dep, depFail);
                errors.add("Node " + dep + " cancelado porque dependência " + failedNodeId + " falhou");
                fila.addAll(adjacencyList.getOrDefault(dep, List.of()));
            }
        }
    }

    private ExtractionResult.NodeResult executarNode(ExecutionPlanRequest plano,
            ExecutionPlanRequest.SerializableNode node) {
        long nodeStart = System.currentTimeMillis();
        log.info("Executando node: {} (recurso: {})", node.getNodeId(), node.getResource());

        ExtractionResult.NodeResult nodeResult = new ExtractionResult.NodeResult();
        nodeResult.setNodeId(node.getNodeId());

        ExtractDataActivity.ExtractRequest extractRequest = new ExtractDataActivity.ExtractRequest();
        extractRequest.setNodeId(node.getNodeId());
        extractRequest.setResource(node.getResource());
        extractRequest.setConsentId(plano.getConsentId());
        extractRequest.setIdempotencyKey(node.getIdempotencyKey());
        extractRequest.setTimeoutSeconds(node.getTimeoutSeconds());

        ExtractDataActivity.ExtractResult extractResult = extractActivity.extract(extractRequest);
        if (extractResult == null || !extractResult.isSuccess()) {
            nodeResult.setSuccess(false);
            nodeResult.setError(extractResult != null ? extractResult.getErrorMessage() : "Extração retornou nulo");
            nodeResult.setDurationMs(System.currentTimeMillis() - nodeStart);
            return nodeResult;
        }
        nodeResult.setRecordsExtracted(extractResult.getRecordCount());

        TransformDataActivity.TransformRequest transformRequest = new TransformDataActivity.TransformRequest();
        transformRequest.setNodeId(node.getNodeId());
        transformRequest.setResource(node.getResource());
        transformRequest.setConsentId(plano.getConsentId());
        transformRequest.setExecutionPlanId(plano.getPlanId());
        transformRequest.setRawData(extractResult.getRawData());
        transformRequest.setRecordCount(extractResult.getRecordCount());

        TransformDataActivity.TransformResult transformResult = transformActivity.transform(transformRequest);
        if (transformResult == null || !transformResult.isSuccess()) {
            nodeResult.setSuccess(false);
            nodeResult.setError(transformResult != null ? transformResult.getErrorMessage() : "Transformação retornou nulo");
            nodeResult.setDurationMs(System.currentTimeMillis() - nodeStart);
            return nodeResult;
        }
        nodeResult.setRecordsTransformed(transformResult.getRecordCount());

        PublishDataActivity.PublishRequest publishRequest = new PublishDataActivity.PublishRequest();
        publishRequest.setNodeId(node.getNodeId());
        publishRequest.setResource(node.getResource());
        publishRequest.setCanonicalData(transformResult.getCanonicalData());
        publishRequest.setLineageId(transformResult.getLineageId());
        publishRequest.setRecordCount(transformResult.getRecordCount());
        publishRequest.setConsentId(plano.getConsentId());
        publishRequest.setExecutionId(plano.getPlanId());
        publishRequest.setSensitivityLevel(transformResult.getSensitivityLevel());

        PublishDataActivity.PublishResult publishResult = publishActivity.publish(publishRequest);
        if (publishResult == null || !publishResult.isSuccess()) {
            nodeResult.setSuccess(false);
            nodeResult.setError(publishResult != null ? publishResult.getErrorMessage() : "Publicação retornou nulo");
            nodeResult.setDurationMs(System.currentTimeMillis() - nodeStart);
            return nodeResult;
        }

        nodeResult.setSuccess(true);
        nodeResult.setRecordsPublished(publishResult.getPublishedCount());
        nodeResult.setDurationMs(System.currentTimeMillis() - nodeStart);

        log.info("Node {} concluído - extraídos: {}, transformados: {}, publicados: {}",
                node.getNodeId(), nodeResult.getRecordsExtracted(),
                nodeResult.getRecordsTransformed(), nodeResult.getRecordsPublished());

        return nodeResult;
    }

    @Override
    public void cancelarExtracao(String motivo) {
        log.info("Recebido sinal de cancelamento: {}", motivo);
        this.cancelSignal = true;
        this.cancelReason = motivo;
    }

    @Override
    public ExtractionStatus getStatus() {
        return status;
    }
}
