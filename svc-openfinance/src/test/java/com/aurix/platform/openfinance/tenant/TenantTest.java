package com.aurix.platform.openfinance.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TenantContext deve gerenciar tenant da thread e fazer fallback para default")
    void deveGerenciarTenantContext() {
        assertThat(TenantContext.getTenantId()).isEqualTo("aurix");

        TenantContext.setTenantId("banco-ouro");
        assertThat(TenantContext.getTenantId()).isEqualTo("banco-ouro");

        TenantContext.clear();
        assertThat(TenantContext.getTenantId()).isEqualTo("aurix");
    }

    @Test
    @DisplayName("TenantFilter deve capturar header X-Tenant-Id e definir header de resposta")
    void deveCapturarHeaderNoFilter() throws Exception {
        TenantProperties props = new TenantProperties();
        props.setId("default-tenant");
        TenantFilter filter = new TenantFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "cooperativa-sul");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-Tenant-Id")).isEqualTo("cooperativa-sul");
        assertThat(TenantContext.getTenantId()).isEqualTo("aurix");
    }
}
