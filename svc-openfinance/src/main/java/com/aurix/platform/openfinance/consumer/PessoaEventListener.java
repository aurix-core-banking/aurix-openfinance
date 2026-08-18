package com.aurix.platform.openfinance.consumer;

import com.aurix.platform.openfinance.entity.PessoaConsentida;
import com.aurix.platform.openfinance.entity.Consentimento;
import com.aurix.platform.openfinance.repository.PessoaConsentidaRepository;
import com.aurix.platform.openfinance.repository.ConsentimentoRepository;
import com.aurix.platform.shared.event.ClienteCriadoEvent;
import com.aurix.platform.shared.event.ClienteAtualizadoEvent;
import com.aurix.platform.shared.event.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PessoaEventListener {

    private static final Logger log = LoggerFactory.getLogger(PessoaEventListener.class);
    private final PessoaConsentidaRepository pessoaRepository;
    private final ConsentimentoRepository consentimentoRepository;

    public PessoaEventListener(PessoaConsentidaRepository pessoaRepository,
                               ConsentimentoRepository consentimentoRepository) {
        this.pessoaRepository = pessoaRepository;
        this.consentimentoRepository = consentimentoRepository;
    }

    @KafkaListener(
        topics = {Topics.CUSTOMER_CLIENTE_CRIADO, Topics.CUSTOMER_CLIENTE_ATUALIZADO},
        groupId = "aurix-openfinance-pessoa-group"
    )
    public void onClienteEvent(Object event) {
        String customerId = null;
        String documento = null;
        String nome = null;

        if (event instanceof ClienteCriadoEvent e) {
            customerId = String.valueOf(e.getClienteId());
            documento = e.getDocumento();
            nome = e.getNome();
            log.info("Evento Open Finance recebido: CLIENTE_CRIADO — cliente={}", customerId);
        } else if (event instanceof ClienteAtualizadoEvent e) {
            customerId = String.valueOf(e.getClienteId());
            documento = e.getDocumento();
            log.info("Evento Open Finance recebido: CLIENTE_ATUALIZADO — cliente={}", customerId);
        }

        if (customerId == null) return;

        List<Consentimento> consentimentosAtivos = consentimentoRepository
            .findByStatus(Consentimento.StatusConsentimento.AUTHORISED);

        for (Consentimento consentimento : consentimentosAtivos) {
            if (consentimento.getDataExpiracao().isBefore(LocalDateTime.now())) continue;

            boolean jaExiste = pessoaRepository
                .findByCustomerIdAndConsentId(customerId, consentimento.getConsentId())
                .isPresent();

            if (!jaExiste) {
                PessoaConsentida pessoa = new PessoaConsentida();
                pessoa.setConsentId(consentimento.getConsentId());
                pessoa.setCustomerId(customerId);
                pessoa.setTipoPessoa(PessoaConsentida.TipoPessoa.FISICA);
                pessoa.setCpfCnpj(documento);
                pessoa.setNomeCompleto(nome);
                pessoa.setDataAtualizacao(LocalDateTime.now());
                pessoaRepository.save(pessoa);
                log.info("Pessoa consentida criada: consentId={}, customerId={}",
                    consentimento.getConsentId(), customerId);
            }
        }
    }
}
