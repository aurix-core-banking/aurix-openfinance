package com.aurix.platform.openfinance.repository;

import com.aurix.platform.openfinance.entity.ConsentimentoVersao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório do histórico imutável de versões de consentimento (INV-006).
 * Apenas inserção — versões nunca são atualizadas nem removidas.
 */
public interface ConsentimentoVersaoRepository extends JpaRepository<ConsentimentoVersao, Long> {

    Optional<ConsentimentoVersao> findByConsentIdAndVersao(String consentId, int versao);

    List<ConsentimentoVersao> findByConsentIdOrderByVersaoAsc(String consentId);

    long countByConsentId(String consentId);
}