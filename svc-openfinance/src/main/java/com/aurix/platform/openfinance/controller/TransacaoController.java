package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.TransacaoResponse;
import com.aurix.platform.openfinance.service.TransacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1/accounts/{accountId}/transactions")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TransacaoResponse>> listar(
            @PathVariable String accountId,
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarTransacoes(accountId, consentId));
    }
}
