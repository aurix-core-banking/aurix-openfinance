package com.aurix.platform.openfinance.observability;

import com.aurix.platform.openfinance.dto.DisponibilidadeResponse;
import com.aurix.platform.openfinance.dto.MetricaDominioResponse;
import com.aurix.platform.openfinance.dto.MetricasSlaResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de consolidação e cálculo de métricas regulatórias do BACEN (Instrução Normativa nº 106).
 * Rastreia disponibilidade, latência percentil (p50, p95, p99) e taxas de erro por domínio.
 */
@Service
public class MetricasRegulatoriasService {

    public static final double META_DISPONIBILIDADE_PERCENTUAL = 99.5;
    public static final double LIMITE_P95_REGULATORIO_MS = 1500.0;
    public static final double LIMITE_P99_REGULATORIO_MS = 3000.0;
    private static final int MAX_AMOSTRAS_LATENCIA = 10000;

    private final Instant inicioOperacao = Instant.now();
    private final AtomicLong totalRequisicoes = new AtomicLong(0);
    private final AtomicLong requisicoesSucesso = new AtomicLong(0);
    private final AtomicLong requisicoesErroCliente = new AtomicLong(0);
    private final AtomicLong requisicoesErroServidor = new AtomicLong(0);

    private final List<Long> amostrasLatencia = new CopyOnWriteArrayList<>();
    private final Map<String, EstatisticasDominio> metricasPorDominio = new ConcurrentHashMap<>();

    /**
     * Registra a execução de uma requisição HTTP da API Open Finance.
     *
     * @param dominio   domínio funcional (ex: consents, accounts, pix, credit-operations)
     * @param statusHttp código HTTP da resposta
     * @param duracaoMs tempo de processamento em milissegundos
     */
    public void registrarRequisicao(String dominio, int statusHttp, long duracaoMs) {
        String dom = (dominio == null || dominio.isBlank()) ? "geral" : dominio.toLowerCase().trim();
        long duracao = Math.max(duracaoMs, 0);

        totalRequisicoes.incrementAndGet();

        boolean erroCliente = statusHttp >= 400 && statusHttp < 500;
        boolean erroServidor = statusHttp >= 500;
        boolean sucesso = statusHttp >= 200 && statusHttp < 400;

        if (sucesso) {
            requisicoesSucesso.incrementAndGet();
        } else if (erroCliente) {
            requisicoesErroCliente.incrementAndGet();
        } else if (erroServidor) {
            requisicoesErroServidor.incrementAndGet();
        }

        if (amostrasLatencia.size() < MAX_AMOSTRAS_LATENCIA) {
            amostrasLatencia.add(duracao);
        }

        metricasPorDominio.computeIfAbsent(dom, d -> new EstatisticasDominio(d))
                .registrar(statusHttp, duracao);
    }

    /**
     * Obtém o relatório regulatório de disponibilidade atual da instituição (IN BACEN 106).
     */
    public DisponibilidadeResponse obterDisponibilidade() {
        long total = totalRequisicoes.get();
        long erros5xx = requisicoesErroServidor.get();

        double disponibilidade = (total == 0) ? 100.0 : Math.max(0.0, ((double) (total - erros5xx) / total) * 100.0);
        disponibilidade = Math.round(disponibilidade * 100.0) / 100.0;

        boolean emConformidade = disponibilidade >= META_DISPONIBILIDADE_PERCENTUAL;
        String statusOperacional = emConformidade ? "OPERACIONAL" : (disponibilidade >= 95.0 ? "DEGRADADO" : "INDISPONIVEL");
        long tempoAtividadeSegundos = Duration.between(inicioOperacao, Instant.now()).getSeconds();

        String periodoReferencia = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        return new DisponibilidadeResponse(
                statusOperacional,
                disponibilidade,
                META_DISPONIBILIDADE_PERCENTUAL,
                emConformidade,
                tempoAtividadeSegundos,
                periodoReferencia,
                Instant.now()
        );
    }

    /**
     * Obtém o relatório regulatório consolidado de SLA e latências da API (IN BACEN 106).
     */
    public MetricasSlaResponse obterMetricasSla() {
        long total = totalRequisicoes.get();
        long sucesso = requisicoesSucesso.get();
        long erro4xx = requisicoesErroCliente.get();
        long erro5xx = requisicoesErroServidor.get();

        double taxaSucesso = (total == 0) ? 100.0 : Math.round(((double) sucesso / total) * 10000.0) / 100.0;

        List<Long> latencias = new ArrayList<>(amostrasLatencia);
        Collections.sort(latencias);

        double media = calcularMedia(latencias);
        double p50 = calcularPercentil(latencias, 0.50);
        double p95 = calcularPercentil(latencias, 0.95);
        double p99 = calcularPercentil(latencias, 0.99);

        DisponibilidadeResponse disp = obterDisponibilidade();
        boolean slaAtendido = disp.emConformidade()
                && p95 <= LIMITE_P95_REGULATORIO_MS
                && p99 <= LIMITE_P99_REGULATORIO_MS;

        Map<String, MetricaDominioResponse> mapaDominios = new ConcurrentHashMap<>();
        for (Map.Entry<String, EstatisticasDominio> entry : metricasPorDominio.entrySet()) {
            mapaDominios.put(entry.getKey(), entry.getValue().toResponse());
        }

        return new MetricasSlaResponse(
                total,
                sucesso,
                taxaSucesso,
                erro4xx,
                erro5xx,
                media,
                p50,
                p95,
                p99,
                LIMITE_P95_REGULATORIO_MS,
                LIMITE_P99_REGULATORIO_MS,
                slaAtendido,
                mapaDominios,
                Instant.now()
        );
    }

    public void reiniciar() {
        totalRequisicoes.set(0);
        requisicoesSucesso.set(0);
        requisicoesErroCliente.set(0);
        requisicoesErroServidor.set(0);
        amostrasLatencia.clear();
        metricasPorDominio.clear();
    }

    private double calcularMedia(List<Long> latencias) {
        if (latencias.isEmpty()) {
            return 0.0;
        }
        long soma = 0;
        for (Long l : latencias) {
            soma += l;
        }
        return Math.round(((double) soma / latencias.size()) * 100.0) / 100.0;
    }

    private double calcularPercentil(List<Long> latenciasOrdenadas, double percentil) {
        if (latenciasOrdenadas.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentil * latenciasOrdenadas.size()) - 1;
        index = Math.max(0, Math.min(index, latenciasOrdenadas.size() - 1));
        return latenciasOrdenadas.get(index);
    }

    /**
     * Estatísticas agregadas de um domínio específico do Open Finance.
     */
    private static class EstatisticasDominio {
        private final String dominio;
        private final AtomicLong total = new AtomicLong(0);
        private final AtomicLong erros = new AtomicLong(0);
        private final List<Long> latencias = new CopyOnWriteArrayList<>();

        public EstatisticasDominio(String dominio) {
            this.dominio = dominio;
        }

        public void registrar(int statusHttp, long duracaoMs) {
            total.incrementAndGet();
            if (statusHttp >= 400) {
                erros.incrementAndGet();
            }
            if (latencias.size() < 2000) {
                latencias.add(duracaoMs);
            }
        }

        public MetricaDominioResponse toResponse() {
            long totalReq = total.get();
            long totalErr = erros.get();
            double taxaErro = (totalReq == 0) ? 0.0 : Math.round(((double) totalErr / totalReq) * 10000.0) / 100.0;

            List<Long> ordenadas = new ArrayList<>(latencias);
            Collections.sort(ordenadas);

            double media = 0.0;
            if (!ordenadas.isEmpty()) {
                long soma = 0;
                for (Long l : ordenadas) {
                    soma += l;
                }
                media = Math.round(((double) soma / ordenadas.size()) * 100.0) / 100.0;
            }

            double p95 = 0.0;
            if (!ordenadas.isEmpty()) {
                int idx = (int) Math.ceil(0.95 * ordenadas.size()) - 1;
                idx = Math.max(0, Math.min(idx, ordenadas.size() - 1));
                p95 = ordenadas.get(idx);
            }

            return new MetricaDominioResponse(
                    dominio,
                    totalReq,
                    media,
                    p95,
                    taxaErro
            );
        }
    }
}
