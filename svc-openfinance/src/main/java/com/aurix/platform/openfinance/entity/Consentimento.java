package com.aurix.platform.openfinance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consentimento")
public class Consentimento {

    public enum StatusConsentimento {
        AWAITING_AUTHORISATION, AUTHORISED, REJECTED, REVOKED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String consentId;

    @Column(nullable = false, length = 128)
    private String clientId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusConsentimento status;

    @Column(nullable = false)
    private String permissions;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAprovacao;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    private String motivoRejeicao;

    @Column(nullable = false)
    private int version;

    public Consentimento() {
        this.status = StatusConsentimento.AWAITING_AUTHORISATION;
        this.version = 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public StatusConsentimento getStatus() { return status; }
    public void setStatus(StatusConsentimento status) { this.status = status; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataAprovacao() { return dataAprovacao; }
    public void setDataAprovacao(LocalDateTime dataAprovacao) { this.dataAprovacao = dataAprovacao; }
    public LocalDateTime getDataExpiracao() { return dataExpiracao; }
    public void setDataExpiracao(LocalDateTime dataExpiracao) { this.dataExpiracao = dataExpiracao; }
    public String getMotivoRejeicao() { return motivoRejeicao; }
    public void setMotivoRejeicao(String motivoRejeicao) { this.motivoRejeicao = motivoRejeicao; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
