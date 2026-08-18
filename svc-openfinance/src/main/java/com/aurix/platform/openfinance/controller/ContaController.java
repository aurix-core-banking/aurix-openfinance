package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.ContaResponse;
import com.aurix.platform.openfinance.service.ContaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1/accounts")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ContaResponse>> listar(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarContas(consentId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<ContaResponse>> buscarPorConta(
            @PathVariable String accountId) {
        return ResponseEntity.ok(service.listarContasPorConta(accountId));
    }

    @GetMapping("/{accountId}/balances")
    public ResponseEntity<List<ContaResponse>> saldo(
            @PathVariable String accountId,
            @RequestHeader("X-Consent-Id") String consentId) {
        List<ContaResponse> contas = service.listarContasPorConta(accountId);
        return ResponseEntity.ok(contas);
    }
}
