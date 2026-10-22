package com.aurix.platform.openfinance.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP para coleta contínua de métricas de SLA e latência das APIs do Open Finance.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class MetricasRegulatoriasFilter extends OncePerRequestFilter {

    private final MetricasRegulatoriasService metricasService;

    public MetricasRegulatoriasFilter(MetricasRegulatoriasService metricasService) {
        this.metricasService = metricasService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Não contabiliza chamadas internas, documentação ou os próprios endpoints de métricas
        if (uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/open-finance/v1/metrics")) {
            filterChain.doFilter(request, response);
            return;
        }

        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracaoMs = System.currentTimeMillis() - inicio;
            String dominio = extrairDominio(uri);
            metricasService.registrarRequisicao(dominio, response.getStatus(), duracaoMs);
        }
    }

    private String extrairDominio(String uri) {
        if (uri == null || !uri.startsWith("/open-finance/v1/")) {
            return "geral";
        }
        String sub = uri.substring("/open-finance/v1/".length());
        int slashIdx = sub.indexOf('/');
        return (slashIdx > 0) ? sub.substring(0, slashIdx) : sub;
    }
}
