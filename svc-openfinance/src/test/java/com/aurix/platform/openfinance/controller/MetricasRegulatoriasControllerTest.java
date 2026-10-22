package com.aurix.platform.openfinance.controller;

import com.aurix.platform.openfinance.dto.DisponibilidadeResponse;
import com.aurix.platform.openfinance.dto.MetricasSlaResponse;
import com.aurix.platform.openfinance.observability.MetricasRegulatoriasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasRegulatoriasControllerTest {

    private MetricasRegulatoriasService service;
    private MetricasRegulatoriasController controller;

    @BeforeEach
    void setUp() {
        service = new MetricasRegulatoriasService();
        service.reiniciar();
        controller = new MetricasRegulatoriasController(service);
    }

    @Test
    @DisplayName("Endpoint /metrics/availability deve retornar status 200 e dados regulatórios")
    void deveRetornarDisponibilidade200() {
        ResponseEntity<DisponibilidadeResponse> response = controller.getDisponibilidade();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        DisponibilidadeResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.statusOperacional()).isEqualTo("OPERACIONAL");
        assertThat(body.taxaDisponibilidadePercentual()).isEqualTo(100.0);
        assertThat(body.metaRegulatoriaPercentual()).isEqualTo(99.5);
        assertThat(body.emConformidade()).isTrue();
    }

    @Test
    @DisplayName("Endpoint /metrics/sla deve retornar status 200 com percentis e limites regulatórios")
    void deveRetornarSla200() {
        service.registrarRequisicao("consents", 200, 45);
        service.registrarRequisicao("accounts", 200, 80);

        ResponseEntity<MetricasSlaResponse> response = controller.getMetricasSla();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        MetricasSlaResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalRequisicoes()).isEqualTo(2);
        assertThat(body.requisicoesSucesso()).isEqualTo(2);
        assertThat(body.limiteP95RegulatorioMs()).isEqualTo(1500.0);
        assertThat(body.limiteP99RegulatorioMs()).isEqualTo(3000.0);
        assertThat(body.slaAtendido()).isTrue();
    }
}
