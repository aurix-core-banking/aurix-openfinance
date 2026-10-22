package com.aurix.platform.openfinance.observability;

import com.aurix.platform.openfinance.dto.DisponibilidadeResponse;
import com.aurix.platform.openfinance.dto.MetricasSlaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasRegulatoriasServiceTest {

    private MetricasRegulatoriasService service;

    @BeforeEach
    void setUp() {
        service = new MetricasRegulatoriasService();
        service.reiniciar();
    }

    @Test
    @DisplayName("Deve iniciar com 100% de disponibilidade e em conformidade")
    void deveIniciarComDisponibilidadeTotal() {
        DisponibilidadeResponse disp = service.obterDisponibilidade();

        assertThat(disp.statusOperacional()).isEqualTo("OPERACIONAL");
        assertThat(disp.taxaDisponibilidadePercentual()).isEqualTo(100.0);
        assertThat(disp.metaRegulatoriaPercentual()).isEqualTo(99.5);
        assertThat(disp.emConformidade()).isTrue();
        assertThat(disp.tempoAtividadeSegundos()).isGreaterThanOrEqualTo(0);
        assertThat(disp.periodoReferencia()).isNotBlank();
    }

    @Test
    @DisplayName("Deve calcular SLA e percentis p50, p95 e p99 corretamente")
    void deveCalcularSlaEPercentis() {
        // Registra 100 chamadas em contas
        for (int i = 1; i <= 100; i++) {
            service.registrarRequisicao("accounts", 200, i * 10); // latências de 10ms até 1000ms
        }

        MetricasSlaResponse sla = service.obterMetricasSla();

        assertThat(sla.totalRequisicoes()).isEqualTo(100);
        assertThat(sla.requisicoesSucesso()).isEqualTo(100);
        assertThat(sla.taxaSucessoPercentual()).isEqualTo(100.0);
        assertThat(sla.requisicoesErroCliente()).isZero();
        assertThat(sla.requisicoesErroServidor()).isZero();
        assertThat(sla.latenciaMediaMs()).isGreaterThan(500.0);
        assertThat(sla.latenciaP50Ms()).isEqualTo(500.0);
        assertThat(sla.latenciaP95Ms()).isEqualTo(950.0);
        assertThat(sla.latenciaP99Ms()).isEqualTo(990.0);
        assertThat(sla.slaAtendido()).isTrue();

        assertThat(sla.metricasPorDominio()).containsKey("accounts");
        assertThat(sla.metricasPorDominio().get("accounts").totalRequisicoes()).isEqualTo(100);
        assertThat(sla.metricasPorDominio().get("accounts").taxaErroPercentual()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Deve acusar não conformidade quando taxa de erro 5xx derrubar disponibilidade abaixo de 99.5%")
    void deveAcusarNaoConformidadePorErrosServidor() {
        // 98 sucessos e 2 erros 500 = 98.0% de disponibilidade (< 99.5%)
        for (int i = 0; i < 98; i++) {
            service.registrarRequisicao("pix", 200, 50);
        }
        service.registrarRequisicao("pix", 500, 120);
        service.registrarRequisicao("pix", 503, 150);

        DisponibilidadeResponse disp = service.obterDisponibilidade();
        assertThat(disp.taxaDisponibilidadePercentual()).isEqualTo(98.0);
        assertThat(disp.emConformidade()).isFalse();
        assertThat(disp.statusOperacional()).isEqualTo("DEGRADADO");

        MetricasSlaResponse sla = service.obterMetricasSla();
        assertThat(sla.slaAtendido()).isFalse();
        assertThat(sla.requisicoesErroServidor()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve contabilizar erros 4xx sem afetar a taxa de disponibilidade do servidor")
    void deveContabilizarErros4xxSemDegradarDisponibilidadeServidor() {
        for (int i = 0; i < 90; i++) {
            service.registrarRequisicao("consents", 200, 40);
        }
        for (int i = 0; i < 10; i++) {
            service.registrarRequisicao("consents", 400, 20); // bad request do cliente
        }

        DisponibilidadeResponse disp = service.obterDisponibilidade();
        assertThat(disp.taxaDisponibilidadePercentual()).isEqualTo(100.0);
        assertThat(disp.emConformidade()).isTrue();

        MetricasSlaResponse sla = service.obterMetricasSla();
        assertThat(sla.requisicoesErroCliente()).isEqualTo(10);
        assertThat(sla.taxaSucessoPercentual()).isEqualTo(90.0);
    }
}
