package com.aurix.platform.openfinance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/open-finance/v1/credit-cards")
public class CartaoCreditoController {

    @GetMapping
    public ResponseEntity<Map<String, String>> listar() {
        return ResponseEntity.ok(Map.of(
            "status", "Fase 2 - BACEN",
            "message", "API de cartões de crédito será implementada na Fase 2 do Open Finance"
        ));
    }
}
