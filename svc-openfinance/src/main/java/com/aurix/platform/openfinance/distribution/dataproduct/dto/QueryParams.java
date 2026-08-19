package com.aurix.platform.openfinance.distribution.dataproduct.dto;

/**
 * Parametros de consulta para servir dados de produto.
 */
public class QueryParams {

    private String filters;
    private Integer limit;
    private Integer offset;

    public QueryParams() {
    }

    public QueryParams(String filters, Integer limit, Integer offset) {
        this.filters = filters;
        this.limit = limit;
        this.offset = offset;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
