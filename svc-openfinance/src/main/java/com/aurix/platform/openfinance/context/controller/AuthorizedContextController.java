package com.aurix.platform.openfinance.context.controller;

import com.aurix.platform.openfinance.context.entity.AuthorizedContext;
import com.aurix.platform.openfinance.context.service.AuthorizedContextService;
import com.aurix.platform.openfinance.policy.dto.PolicyDecisionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contexts")
public class AuthorizedContextController {

    private final AuthorizedContextService contextService;

    public AuthorizedContextController(AuthorizedContextService contextService) {
        this.contextService = contextService;
    }

    @PostMapping
    public ResponseEntity<AuthorizedContext> create(
            @RequestBody CreateContextRequest request) {
        AuthorizedContext context = contextService.create(
                request.policyDecision,
                request.subject,
                request.purpose,
                request.permissions,
                request.resourceGraph,
                request.signingAlgorithm,
                request.dpopThumbprint
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(context);
    }

    @GetMapping("/{contextId}")
    public ResponseEntity<AuthorizedContext> get(@PathVariable String contextId) {
        return ResponseEntity.ok(contextService.getByContextId(contextId));
    }

    @PostMapping("/{contextId}/validate")
    public ResponseEntity<Map<String, Object>> validate(@PathVariable String contextId) {
        AuthorizedContext context = contextService.validate(contextId);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "contextId", context.getContextId(),
                "subject", context.getSubject(),
                "consentId", context.getConsentId()
        ));
    }

    @PostMapping("/{contextId}/revoke")
    public ResponseEntity<AuthorizedContext> revoke(@PathVariable String contextId) {
        return ResponseEntity.ok(contextService.revoke(contextId));
    }

    @GetMapping("/consent/{consentId}")
    public ResponseEntity<List<AuthorizedContext>> listByConsent(
            @PathVariable String consentId) {
        return ResponseEntity.ok(contextService.listActiveByConsentId(consentId));
    }

    public static class CreateContextRequest {
        public PolicyDecisionResponse policyDecision;
        public String subject;
        public String purpose;
        public String permissions;
        public String resourceGraph;
        public String signingAlgorithm;
        public String dpopThumbprint;
    }
}
