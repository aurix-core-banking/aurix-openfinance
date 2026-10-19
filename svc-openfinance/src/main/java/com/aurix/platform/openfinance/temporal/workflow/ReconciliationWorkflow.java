package com.aurix.platform.openfinance.temporal.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow Temporal de reconciliação pós-extração e reparo automático
 * declarado em {@code architecture/services.yaml} ({@code ReconciliationWorkflow})
 * e {@code architecture/flows.yaml} ({@code reconciliation}).
 */
@WorkflowInterface
public interface ReconciliationWorkflow {

    @WorkflowMethod
    ReconciliationWorkflowResult reconciliar(ReconciliationWorkflowInput input);

    @QueryMethod
    String getFaseAtual();

    class ReconciliationWorkflowInput {
        private String planId;
        private Map<String, Integer> extractedCounts = new LinkedHashMap<>();
        private Map<String, Integer> publishedCounts = new LinkedHashMap<>();
        private boolean autoRepair = true;

        public ReconciliationWorkflowInput() {
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public Map<String, Integer> getExtractedCounts() {
            return extractedCounts;
        }

        public void setExtractedCounts(Map<String, Integer> extractedCounts) {
            this.extractedCounts = extractedCounts;
        }

        public Map<String, Integer> getPublishedCounts() {
            return publishedCounts;
        }

        public void setPublishedCounts(Map<String, Integer> publishedCounts) {
            this.publishedCounts = publishedCounts;
        }

        public boolean isAutoRepair() {
            return autoRepair;
        }

        public void setAutoRepair(boolean autoRepair) {
            this.autoRepair = autoRepair;
        }
    }

    class ReconciliationWorkflowResult {
        private String planId;
        private boolean hasDivergences;
        private boolean repaired;
        private int totalRecordsRepaired;
        private List<String> divergences = new ArrayList<>();

        public ReconciliationWorkflowResult() {
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public boolean isHasDivergences() {
            return hasDivergences;
        }

        public void setHasDivergences(boolean hasDivergences) {
            this.hasDivergences = hasDivergences;
        }

        public boolean isRepaired() {
            return repaired;
        }

        public void setRepaired(boolean repaired) {
            this.repaired = repaired;
        }

        public int getTotalRecordsRepaired() {
            return totalRecordsRepaired;
        }

        public void setTotalRecordsRepaired(int totalRecordsRepaired) {
            this.totalRecordsRepaired = totalRecordsRepaired;
        }

        public List<String> getDivergences() {
            return divergences;
        }

        public void setDivergences(List<String> divergences) {
            this.divergences = divergences;
        }
    }
}
