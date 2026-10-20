package com.aurix.platform.openfinance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração OpenAPI 3.0 / Swagger para as APIs Open Finance do Aurix Core Banking.
 * Em total conformidade com os padrões técnicos do Banco Central do Brasil (BACEN)
 * e especificações FAPI 1.0 Advanced / RFC 9449 (DPoP).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI configurarOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Aurix Open Finance — Plataforma Bancária Regulamentada")
                .description("Conjunto oficial de APIs Open Finance Brasil reguladas pelo Banco Central do Brasil (BACEN) " +
                    "e Conselho Monetário Nacional (CMN - Resolução Conjunta nº 1/2020).\n\n" +
                    "### Escopos e Fases Implementadas:\n" +
                    "- **Fase 1 (Canais e Produtos)**: Descoberta de recursos e catálogo de participantes.\n" +
                    "- **Fase 2 (Compartilhamento de Dados)**: Consentimento, Contas, Extratos, Cartões de Crédito e Investimentos.\n" +
                    "- **Fase 3 (Iniciação de Pagamentos)**: Iniciação de Pix e pagamentos com verificação e liquidação ponta a ponta.\n" +
                    "- **Fase 4 (Serviços e Câmbio)**: Seguros e produtos de investimento avançados.\n\n" +
                    "### Padrões de Segurança:\n" +
                    "- **OAuth 2.0 / FAPI 1.0 Advanced**: Autenticação mTLS e tokens JWT com criptografia.\n" +
                    "- **DPoP (RFC 9449)**: Demonstração de Prova de Posse (Proof-of-Possession) obrigatória nos endpoints transacionais.\n" +
                    "- **Invariantes Arquiteturais**: INV-001 (No Consent No Execution), INV-002 (No Authorized Resource No Access), " +
                    "INV-003 (No Lineage No Publication), INV-005 (Immutable Authorized Context).")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Equipe de Arquitetura & Governança Aurix Core Banking")
                    .email("openfinance@aurix.com.br")
                    .url("https://github.com/aurix-core-banking/aurix-openfinance"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server().url("/").description("Gateway Unificado Traefik (Ambiente Padrão)"),
                new Server().url("http://localhost:8096").description("Acesso Direto ao Microserviço Open Finance (Desenvolvimento Local)")
            ))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth").addList("DPoPProof"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token de acesso OAuth 2.0 Bearer emitido com escopos regulatórios Open Finance."))
                .addSecuritySchemes("DPoPProof", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("DPoP")
                    .description("Header DPoP contendo JWT assinado pela chave privada efêmera do cliente (RFC 9449).")));
    }
}
