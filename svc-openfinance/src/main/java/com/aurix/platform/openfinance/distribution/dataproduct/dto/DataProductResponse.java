package com.aurix.platform.openfinance.distribution.dataproduct.dto;

import java.util.List;

/**
 * Response de servir dados de um produto de dado.
 */
public class DataProductResponse {

    private String productId;
    private String name;
    private String domain;
    private String format;
    private List<Object> records;
    private int totalCount;

    public DataProductResponse() {
    }

    public DataProductResponse(String productId, String name, String domain,
                                String format, List<Object> records, int totalCount) {
        this.productId = productId;
        this.name = name;
        this.domain = domain;
        this.format = format;
        this.records = records;
        this.totalCount = totalCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<Object> getRecords() {
        return records;
    }

    public void setRecords(List<Object> records) {
        this.records = records;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public static class Builder {
        private String productId;
        private String name;
        private String domain;
        private String format;
        private List<Object> records;
        private int totalCount;

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder records(List<Object> records) {
            this.records = records;
            return this;
        }

        public Builder totalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public DataProductResponse build() {
            return new DataProductResponse(productId, name, domain, format, records, totalCount);
        }
    }
}
