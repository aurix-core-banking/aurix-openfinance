package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.SeguroResponse;
import com.aurix.platform.openfinance.service.SeguroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1")
public class SeguroController {

    private final SeguroService service;

    public SeguroController(SeguroService service) { this.service = service; }

    @GetMapping("/insurance")
    public ResponseEntity<List<SeguroResponse>> listarSeguros(
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listar(consentId));
    }

    @GetMapping("/insurance/{insuranceId}")
    public ResponseEntity<SeguroResponse> buscarSeguro(
            @PathVariable String insuranceId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return service.buscar(consentId, insuranceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String extrairConsentId(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
