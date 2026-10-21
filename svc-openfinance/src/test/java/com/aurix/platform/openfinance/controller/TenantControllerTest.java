package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.TenantInfoResponse;
import com.aurix.platform.openfinance.extractor.connector.CoreBankingConnectorRegistry;
import com.aurix.platform.openfinance.extractor.connector.SandboxCoreBankingConnector;
import com.aurix.platform.openfinance.tenant.TenantContext;
import com.aurix.platform.openfinance.tenant.TenantProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantControllerTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TenantController deve expor dados institucionais do tenant ativo")
    void deveExporInfoNoController() {
        TenantProperties props = new TenantProperties();
        props.setId("fintech-brasil");
        props.setName("Fintech Brasil SCD S.A.");
        props.setCnpj("11.222.333/0001-44");
        props.setParticipantCode("87654321");
        props.setBrandName("Fintech Brasil");
        props.setPrimaryColor("#00CC66");

        CoreBankingConnectorRegistry registry = new CoreBankingConnectorRegistry(
                List.of(new SandboxCoreBankingConnector()), "sandbox");

        TenantController controller = new TenantController(props, registry);

        TenantContext.setTenantId("fintech-brasil");
        ResponseEntity<TenantInfoResponse> response = controller.getInfo();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        TenantInfoResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.tenantId()).isEqualTo("fintech-brasil");
        assertThat(body.razaoSocial()).isEqualTo("Fintech Brasil SCD S.A.");
        assertThat(body.cnpj()).isEqualTo("11.222.333/0001-44");
        assertThat(body.nomeMarca()).isEqualTo("Fintech Brasil");
        assertThat(body.corPrimaria()).isEqualTo("#00CC66");
    }
}
