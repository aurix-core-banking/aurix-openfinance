package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.PixPaymentInitiationRequest;
import com.aurix.platform.openfinance.dto.PixPaymentInitiationResponse;
import com.aurix.platform.openfinance.dto.PixResponse;
import com.aurix.platform.openfinance.service.PixService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST oficial para Iniciação de Pagamentos Pix (Fase 3 Open Finance Brasil)
 * e consulta de transferências Pix compartilhadas (Fase 2/3).
 */
@RestController
@RequestMapping("/open-finance/v1")
public class PixController {

    private final PixService service;

    public PixController(PixService service) {
        this.service = service;
    }

    /**
     * Iniciação de Pagamento Pix (Fase 3 Open Finance Brasil).
     * Exige consentimento regulatório autorizado (INV-001).
     */
    @PostMapping("/pix/payments")
    public ResponseEntity<PixPaymentInitiationResponse> iniciarPagamento(
            @Valid @RequestBody PixPaymentInitiationRequest request,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String authDate,
            @RequestHeader(value = "x-fapi-auth-ip-address", required = false) String ipAddress,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String interactionId,
            @RequestHeader(value = "x-idempotency-key", required = false) String idempotencyKey) {
        PixPaymentInitiationResponse response = service.iniciarPagamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta de status de pagamento Pix iniciado (Fase 3 Open Finance Brasil).
     */
    @GetMapping("/pix/payments/{paymentId}")
    public ResponseEntity<PixPaymentInitiationResponse> consultarPagamento(
            @PathVariable String paymentId,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String authDate,
            @RequestHeader(value = "x-fapi-auth-ip-address", required = false) String ipAddress,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String interactionId) {
        return service.buscarPagamento(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pix/credit-transfers")
    public ResponseEntity<List<PixResponse>> listarPix(
            @RequestHeader(value = "x-fapi-auth-date", required = false) String authDate,
            @RequestHeader(value = "x-fapi-auth-ip-address", required = false) String ipAddress,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String interactionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listar(consentId));
    }

    @GetMapping("/pix/credit-transfers/{pixId}")
    public ResponseEntity<PixResponse> buscarPix(
            @PathVariable String pixId,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String authDate,
            @RequestHeader(value = "x-fapi-auth-ip-address", required = false) String ipAddress,
            @RequestHeader(value = "x-fapi-interaction-id", required = false) String interactionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String consentId = extrairConsentId(authorization);
        return service.listar(consentId).stream()
                .filter(p -> p.getPixId().equals(pixId))
                .findFirst()
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
