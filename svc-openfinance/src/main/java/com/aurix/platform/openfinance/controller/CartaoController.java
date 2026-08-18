package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.*;
import com.aurix.platform.openfinance.service.CartaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1")
public class CartaoController {

    private final CartaoService service;

    public CartaoController(CartaoService service) { this.service = service; }

    @GetMapping("/credit-cards")
    public ResponseEntity<List<CartaoResponse>> listarCartoes(
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listarCartoes(consentId));
    }

    @GetMapping("/credit-cards/{creditCardId}")
    public ResponseEntity<CartaoResponse> buscarCartao(
            @PathVariable String creditCardId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return service.buscarCartao(consentId, creditCardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/credit-cards/{creditCardId}/bills")
    public ResponseEntity<List<FaturaResponse>> listarFaturas(
            @PathVariable String creditCardId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listarFaturas(consentId, creditCardId));
    }

    @GetMapping("/credit-cards/{creditCardId}/bills/{billId}")
    public ResponseEntity<FaturaResponse> buscarFatura(
            @PathVariable String creditCardId,
            @PathVariable String billId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return service.buscarFatura(consentId, creditCardId, billId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/credit-cards/{creditCardId}/transactions")
    public ResponseEntity<List<TransacaoCartaoResponse>> listarTransacoesCartao(
            @PathVariable String creditCardId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listarTransacoesCartao(consentId, creditCardId));
    }

    private String extrairConsentId(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
