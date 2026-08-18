package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pessoa_consentida")
public class PessoaConsentida {

    public enum TipoPessoa {
        FISICA, JURIDICA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consentId;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPessoa tipoPessoa;

    @Column(length = 20)
    private String cpfCnpj;

    @Column(length = 200)
    private String nomeCompleto;

    private LocalDate dataNascimento;

    @Column(length = 10)
    private String sexo;

    @Column(length = 200)
    private String nomeMae;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 200)
    private String logradouro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 5)
    private String estado;

    @Column(length = 10)
    private String cep;

    @Column(length = 50)
    private String pais;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public TipoPessoa getTipoPessoa() { return tipoPessoa; }
    public void setTipoPessoa(TipoPessoa tipoPessoa) { this.tipoPessoa = tipoPessoa; }
    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getNomeMae() { return nomeMae; }
    public void setNomeMae(String nomeMae) { this.nomeMae = nomeMae; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
