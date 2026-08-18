package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.PixResponse;
import com.aurix.platform.openfinance.service.PixService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1")
public class PixController {

    private final PixService service;

    public PixController(PixService service) { this.service = service; }

    @GetMapping("/pix/credit-transfers")
    public ResponseEntity<List<PixResponse>> listarPix(
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
        String consentId = extrairConsentId(authorization);
        return ResponseEntity.ok(service.listar(consentId));
    }

    @GetMapping("/pix/credit-transfers/{pixId}")
    public ResponseEntity<PixResponse> buscarPix(
            @PathVariable String pixId,
            @RequestHeader("x-fapi-auth-date") String authDate,
            @RequestHeader("x-fapi-auth-ip-address") String ipAddress,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader("Authorization") String authorization) {
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
