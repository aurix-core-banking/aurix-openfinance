package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.ConsentimentoRequest;
import com.aurix.platform.openfinance.dto.ConsentimentoResponse;
import com.aurix.platform.openfinance.service.ConsentimentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1/consents")
public class ConsentimentoController {

    private final ConsentimentoService service;

    public ConsentimentoController(ConsentimentoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConsentimentoResponse> criar(
            @Valid @RequestBody ConsentimentoRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ConsentimentoResponse response = service.criar(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{consentId}")
    public ResponseEntity<ConsentimentoResponse> buscar(@PathVariable String consentId) {
        return ResponseEntity.ok(service.buscar(consentId));
    }

    @PostMapping("/{consentId}/authorise")
    public ResponseEntity<ConsentimentoResponse> aprovar(@PathVariable String consentId) {
        return ResponseEntity.ok(service.aprovar(consentId));
    }

    @PostMapping("/{consentId}/reject")
    public ResponseEntity<ConsentimentoResponse> rejeitar(
            @PathVariable String consentId,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(service.rejeitar(consentId, motivo));
    }

    @PostMapping("/{consentId}/revoke")
    public ResponseEntity<ConsentimentoResponse> revogar(
            @PathVariable String consentId,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(service.revogar(consentId, motivo));
    }

    @GetMapping
    public ResponseEntity<List<ConsentimentoResponse>> listar(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(List.of());
    }
}
