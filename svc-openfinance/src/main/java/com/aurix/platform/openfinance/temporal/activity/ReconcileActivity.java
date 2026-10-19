package com.aurix.platform.openfinance.temporal.activity;

import io.temporal.activity.ActivityInterface;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atividade Temporal de reconciliação pós-publicação e reparo de divergências
 * conforme declarado em {@code architecture/services.yaml} e {@code architecture/flows.yaml}.
 */
@ActivityInterface
public interface ReconcileActivity {

    ReconcileOutput reconcile(ReconcileInput input);

    RepairOutput repair(String reconciliationId);

    class ReconcileInput {
        private String planId;
        private Map<String, Integer> extractedCounts = new LinkedHashMap<>();
        private Map<String, Integer> publishedCounts = new LinkedHashMap<>();

        public ReconcileInput() {
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
    }

    class ReconcileOutput {
        private String planId;
        private boolean hasDivergences;
        private List<String> divergences = new ArrayList<>();
        private List<String> divergentReconciliationIds = new ArrayList<>();

        public ReconcileOutput() {
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

        public List<String> getDivergences() {
            return divergences;
        }

        public void setDivergences(List<String> divergences) {
            this.divergences = divergences;
        }

        public List<String> getDivergentReconciliationIds() {
            return divergentReconciliationIds;
        }

        public void setDivergentReconciliationIds(List<String> divergentReconciliationIds) {
            this.divergentReconciliationIds = divergentReconciliationIds;
        }
    }

    class RepairOutput {
        private String reconciliationId;
        private boolean success;
        private int recordsRepaired;
        private String errorMessage;

        public RepairOutput() {
        }

        public String getReconciliationId() {
            return reconciliationId;
        }

        public void setReconciliationId(String reconciliationId) {
            this.reconciliationId = reconciliationId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getRecordsRepaired() {
            return recordsRepaired;
        }

        public void setRecordsRepaired(int recordsRepaired) {
            this.recordsRepaired = recordsRepaired;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
