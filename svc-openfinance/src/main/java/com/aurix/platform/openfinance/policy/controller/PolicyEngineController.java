package com.aurix.platform.openfinance.policy.controller;

import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import com.aurix.platform.openfinance.policy.dto.PolicyEvaluationRequest;
import com.aurix.platform.openfinance.policy.entity.PolicyDecision;
import com.aurix.platform.openfinance.policy.entity.PolicyRule;
import com.aurix.platform.openfinance.policy.service.PolicyEngineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policy")
public class PolicyEngineController {

    private final PolicyEngineService policyEngineService;

    public PolicyEngineController(PolicyEngineService policyEngineService) {
        this.policyEngineService = policyEngineService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<PolicyDecisionResponse> evaluate(
            @RequestBody PolicyEvaluationRequest request) {
        PolicyDecisionResponse response = policyEngineService.evaluate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<PolicyRule>> listActiveRules() {
        return ResponseEntity.ok(policyEngineService.listActiveRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<PolicyRule> createRule(@RequestBody PolicyRule rule) {
        PolicyRule created = policyEngineService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/decisions")
    public ResponseEntity<List<PolicyDecision>> getAuditTrail(
            @RequestParam String consentId) {
        return ResponseEntity.ok(policyEngineService.getAuditTrail(consentId));
    }
}
