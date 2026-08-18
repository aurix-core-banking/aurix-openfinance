package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.ContatoResponse;
import com.aurix.platform.openfinance.dto.EnderecoResponse;
import com.aurix.platform.openfinance.dto.PessoaResponse;
import com.aurix.platform.openfinance.service.PessoaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/open-finance/v1/customers")
public class PessoaController {

    private final PessoaService service;

    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @GetMapping("/personal/identifications")
    public ResponseEntity<List<PessoaResponse>> listarPessoasFisicas(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarPessoas(consentId));
    }

    @GetMapping("/personal/identifications/{customerId}")
    public ResponseEntity<PessoaResponse> buscarPessoaFisica(
            @PathVariable String customerId,
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.buscarPessoa(customerId, consentId));
    }

    @GetMapping("/personal/addresses")
    public ResponseEntity<List<EnderecoResponse>> listarEnderecosPessoais(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarEnderecos(consentId));
    }

    @GetMapping("/personal/phone-numbers")
    public ResponseEntity<List<ContatoResponse>> listarTelefones(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarContatos(consentId));
    }

    @GetMapping("/personal/email")
    public ResponseEntity<List<ContatoResponse>> listarEmails(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarContatos(consentId));
    }

    @GetMapping("/business/identifications")
    public ResponseEntity<List<PessoaResponse>> listarPessoasJuridicas(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarPessoas(consentId));
    }

    @GetMapping("/business/identifications/{customerId}")
    public ResponseEntity<PessoaResponse> buscarPessoaJuridica(
            @PathVariable String customerId,
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.buscarPessoa(customerId, consentId));
    }

    @GetMapping("/business/addresses")
    public ResponseEntity<List<EnderecoResponse>> listarEnderecosEmpresariais(
            @RequestHeader("X-Consent-Id") String consentId) {
        return ResponseEntity.ok(service.listarEnderecos(consentId));
    }
}
