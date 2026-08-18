package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.EmprestimoResponse;
import com.aurix.platform.openfinance.service.EmprestimoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1")
public class EmprestimoController {

    private final EmprestimoService service;

    public EmprestimoController(EmprestimoService service) { this.service = service; }

    @GetMapping("/loans")
    public ResponseEntity<List<EmprestimoResponse>> listarEmprestimos(
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listar(consentId));
    }

    @GetMapping("/loans/{loanId}")
    public ResponseEntity<EmprestimoResponse> buscarEmprestimo(
            @PathVariable String loanId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return service.buscar(consentId, loanId)
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
