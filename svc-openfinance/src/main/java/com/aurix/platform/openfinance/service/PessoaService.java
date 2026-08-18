package com.aurix.platform.openfinance.service;

import com.aurix.platform.openfinance.dto.EnderecoResponse;
import com.aurix.platform.openfinance.dto.PessoaResponse;
import com.aurix.platform.openfinance.dto.ContatoResponse;
import com.aurix.platform.openfinance.entity.PessoaConsentida;
import com.aurix.platform.openfinance.repository.PessoaConsentidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PessoaService {

    private static final Logger log = LoggerFactory.getLogger(PessoaService.class);
    private final PessoaConsentidaRepository pessoaRepository;
    private final ConsentimentoService consentimentoService;

    public PessoaService(PessoaConsentidaRepository pessoaRepository,
                         ConsentimentoService consentimentoService) {
        this.pessoaRepository = pessoaRepository;
        this.consentimentoService = consentimentoService;
    }

    public List<PessoaResponse> listarPessoas(String consentId) {
        if (!consentimentoService.verificarConsentimentoAtivo(consentId)) {
            throw new SecurityException("Consentimento inválido ou expirado: " + consentId);
        }
        log.info("Listando pessoas consentidas: {}", consentId);
        return pessoaRepository.findByConsentId(consentId).stream()
            .map(this::toPessoaResponse).toList();
    }

    public PessoaResponse buscarPessoa(String customerId, String consentId) {
        if (!consentimentoService.verificarConsentimentoAtivo(consentId)) {
            throw new SecurityException("Consentimento inválido ou expirado: " + consentId);
        }
        PessoaConsentida p = pessoaRepository.findByCustomerIdAndConsentId(customerId, consentId)
            .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada: " + customerId));
        return toPessoaResponse(p);
    }

    public List<EnderecoResponse> listarEnderecos(String consentId) {
        if (!consentimentoService.verificarConsentimentoAtivo(consentId)) {
            throw new SecurityException("Consentimento inválido ou expirado: " + consentId);
        }
        return pessoaRepository.findByConsentId(consentId).stream()
            .map(this::toEnderecoResponse).toList();
    }

    public List<ContatoResponse> listarContatos(String consentId) {
        if (!consentimentoService.verificarConsentimentoAtivo(consentId)) {
            throw new SecurityException("Consentimento inválido ou expirado: " + consentId);
        }
        return pessoaRepository.findByConsentId(consentId).stream()
            .map(this::toContatoResponse).toList();
    }

    private PessoaResponse toPessoaResponse(PessoaConsentida p) {
        PessoaResponse r = new PessoaResponse();
        r.setCustomerId(p.getCustomerId());
        r.setTipoPessoa(p.getTipoPessoa().name());
        r.setCpfCnpj(p.getCpfCnpj());
        r.setNomeCompleto(p.getNomeCompleto());
        r.setDataNascimento(p.getDataNascimento());
        r.setSexo(p.getSexo());
        r.setNomeMae(p.getNomeMae());
        return r;
    }

    private EnderecoResponse toEnderecoResponse(PessoaConsentida p) {
        EnderecoResponse r = new EnderecoResponse();
        r.setLogradouro(p.getLogradouro());
        r.setCidade(p.getCidade());
        r.setEstado(p.getEstado());
        r.setCep(p.getCep());
        r.setPais(p.getPais());
        return r;
    }

    private ContatoResponse toContatoResponse(PessoaConsentida p) {
        ContatoResponse r = new ContatoResponse();
        r.setEmail(p.getEmail());
        r.setTelefone(p.getTelefone());
        return r;
    }
}
