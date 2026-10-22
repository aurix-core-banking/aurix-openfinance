package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.DcrClientRegistrationRequest;
import com.aurix.platform.openfinance.dto.DcrClientRegistrationResponse;
import com.aurix.platform.openfinance.trust.service.DcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de Registro Dinâmico de Clientes (Dynamic Client Registration - RFC 7591 / FAPI-BR).
 */
@RestController
@RequestMapping("/open-finance/v1/dcr")
@Tag(name = "DCR - Dynamic Client Registration", description = "Registro dinâmico de clientes TPP e validação de SSA do Diretório Central")
public class DcrController {

    private final DcrService dcrService;

    public DcrController(DcrService dcrService) {
        this.dcrService = dcrService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar dinamicamente um cliente TPP a partir do Software Statement Assertion (SSA)")
    public ResponseEntity<DcrClientRegistrationResponse> registrarCliente(
            @RequestBody DcrClientRegistrationRequest request) {
        DcrClientRegistrationResponse response = dcrService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Consultar metadados e configuração de um cliente DCR registrado")
    public ResponseEntity<DcrClientRegistrationResponse> consultarCliente(
            @PathVariable("clientId") String clientId) {
        return dcrService.buscarPorClientId(clientId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
