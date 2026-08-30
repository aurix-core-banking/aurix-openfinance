package com.aurix.platform.openfinance.trust.controller;

import com.aurix.platform.openfinance.trust.dto.TrustResult;
import com.aurix.platform.openfinance.trust.receipt.entity.ConsentReceipt;
import com.aurix.platform.openfinance.trust.receipt.repository.ConsentReceiptRepository;
import com.aurix.platform.openfinance.trust.receipt.service.ConsentReceiptService;
import com.aurix.platform.openfinance.trust.service.EcDsaSigningService;
import com.aurix.platform.openfinance.trust.service.TrustPlaneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints de observação e verificação do Trust Plane.
 */
@RestController
@RequestMapping("/api/v1/trust")
public class TrustPlaneController {

    private final TrustPlaneService trustPlaneService;
    private final ConsentReceiptService consentReceiptService;
    private final ConsentReceiptRepository consentReceiptRepository;
    private final EcDsaSigningService ecDsaSigningService;

    public TrustPlaneController(TrustPlaneService trustPlaneService,
                                ConsentReceiptService consentReceiptService,
                                ConsentReceiptRepository consentReceiptRepository,
                                EcDsaSigningService ecDsaSigningService) {
        this.trustPlaneService = trustPlaneService;
        this.consentReceiptService = consentReceiptService;
        this.consentReceiptRepository = consentReceiptRepository;
        this.ecDsaSigningService = ecDsaSigningService;
    }

    /**
     * Verifica a confiança de uma requisição (canal mTLS + DPoP) — dev/teste.
     */
    @PostMapping("/verify")
    public ResponseEntity<TrustResult> verify(
            @RequestHeader(value = "DPoP", required = false) String dpopProof,
            @RequestHeader(value = "X-Client-Cert-Subject", required = false) String subject,
            @RequestHeader(value = "X-Access-Token-Jkt", required = false) String accessTokenJkt,
            jakarta.servlet.http.HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Client-Cert-Subject", subject);
        TrustResult result = trustPlaneService.verify(headers, request.getMethod(),
                request.getRequestURL().toString(), accessTokenJkt, dpopProof);
        return ResponseEntity.ok(result);
    }

    /**
     * Status geral do Trust Plane (algoritmos de assinatura + chave pública).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new HashMap<>();
        body.put("signingAlgorithms", java.util.List.of("ES256", "ES384", "RS256"));
        body.put("publicKeyBase64Url",
                ecDsaSigningService.getPublicKeyB64Url("ES256"));
        body.put("dpOf", "rfc-9449");
        body.put("channel", "gateway-header (dev) / mtls (prod)");
        return ResponseEntity.ok(body);
    }

    /**
     * Busca o recibo de consentimento de um consentimento.
     */
    @GetMapping("/receipts/{consentId}")
    public ResponseEntity<Map<String, Object>> getReceipt(@PathVariable String consentId) {
        return consentReceiptRepository.findByConsentId(consentId)
                .map(r -> ResponseEntity.ok(toReceiptMap(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Verifica criptograficamente um recibo de consentimento (não-repúdio).
     */
    @PostMapping("/receipts/{consentId}/verify")
    public ResponseEntity<Map<String, Object>> verifyReceipt(@PathVariable String consentId) {
        boolean valid = consentReceiptService.verificarRecibo(consentId);
        Map<String, Object> body = new HashMap<>();
        body.put("consentId", consentId);
        body.put("valid", valid);
        body.put("algorithm", "ES256");
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toReceiptMap(ConsentReceipt r) {
        Map<String, Object> map = new HashMap<>();
        map.put("receiptId", r.getReceiptId());
        map.put("consentId", r.getConsentId());
        map.put("contentHash", r.getContentHash());
        map.put("timestamp", r.getTimestamp().toString());
        map.put("signingAlgorithm", r.getSigningAlgorithm());
        map.put("signature", r.getSignature());
        return map;
    }
}
