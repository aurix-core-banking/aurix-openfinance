package com.aurix.platform.openfinance.dto;

import java.time.LocalDate;

public class PessoaResponse {

    private String customerId;
    private String tipoPessoa;
    private String cpfCnpj;
    private String nomeCompleto;
    private LocalDate dataNascimento;
    private String sexo;
    private String nomeMae;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getTipoPessoa() { return tipoPessoa; }
    public void setTipoPessoa(String tipoPessoa) { this.tipoPessoa = tipoPessoa; }
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
}
