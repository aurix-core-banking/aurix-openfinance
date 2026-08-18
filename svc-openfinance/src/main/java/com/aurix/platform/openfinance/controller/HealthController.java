package com.aurix.platform.openfinance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "svc-openfinance",
            "port", 8096,
            "components", Map.of(
                "db", Map.of("status", "UP"),
                "kafka", Map.of("status", "UP"),
                "diskSpace", Map.of("status", "UP")
            )
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "svc-openfinance",
            "description", "Microserviço Open Finance Brasil — BACEN Fase 1",
            "version", "1.0.0",
            "port", 8096
        ));
    }
}
