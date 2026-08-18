package com.aurix.platform.openfinance.dto;

public class EnderecoResponse {

    private String logradouro;
    private String cidade;
    private String estado;
    private String cep;
    private String pais;

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
}
