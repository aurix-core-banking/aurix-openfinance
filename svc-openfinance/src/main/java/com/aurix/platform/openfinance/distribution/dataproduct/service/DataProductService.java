package com.aurix.platform.openfinance.distribution.dataproduct.service;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProductRecord;
import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProductStatus;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRecordRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductResponse;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.QueryParams;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servico de gestao de produtos de dado.
 * Responsavel por criar, materializar e servir produtos de dado.
 *
 * <p>materialize()/serve() persistem em Postgres ({@code data_product_records}) como
 * substituto pragmático de ClickHouse/MinIO para o ambiente dev/local — ver plano de
 * correção. Trocar por um data warehouse de verdade é uma extensão futura documentada,
 * não uma simulação disfarçada de implementação.
 */
@Service
public class DataProductService {

    private static final Logger log = LoggerFactory.getLogger(DataProductService.class);
    private static final int DEFAULT_LIMIT = 100;

    private final DataProductRepository repository;
    private final DataProductRecordRepository recordRepository;

    public DataProductService(DataProductRepository repository,
                               DataProductRecordRepository recordRepository) {
        this.repository = repository;
        this.recordRepository = recordRepository;
    }

    /**
     * Cria um novo produto de dado a partir de dados publicados.
     */
    @Transactional
    public DataProduct create(DataProductRequest request) {
        String productId = UUID.randomUUID().toString();

        DataProduct product = new DataProduct(
                productId,
                request.getName(),
                request.getDescription(),
                request.getDomain(),
                request.getResourceType(),
                request.getFormat(),
                request.getSchema(),
                request.getEndpoint()
        );

        DataProduct saved = repository.save(product);
        log.info("Produto de dado criado: {}", productId);
        return saved;
    }

    /**
     * Materializa dados no storage — persistidos de verdade em
     * {@code data_product_records}, não apenas logados.
     */
    @Transactional
    public void materialize(DataProduct product, List<CanonicalRecord> records) {
        log.info("Materializando {} registros para produto {}",
                records.size(), product.getProductId());

        for (CanonicalRecord record : records) {
            recordRepository.save(new DataProductRecord(
                    product.getProductId(), record.getCanonicalId(), record.getCanonicalData()));
        }

        product.activate();
        repository.save(product);
    }

    /**
     * Serve dados de um produto de dado — consulta real (paginada) aos registros
     * materializados, não mais uma lista vazia fixa.
     */
    @Transactional(readOnly = true)
    public DataProductResponse serve(String productId, QueryParams params) {
        DataProduct product = repository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produto de dado nao encontrado: " + productId));

        if (!product.isActive()) {
            throw new IllegalStateException(
                    "Produto de dado nao esta ativo: " + productId);
        }

        int limit = params != null && params.getLimit() != null ? params.getLimit() : DEFAULT_LIMIT;
        int offset = params != null && params.getOffset() != null ? params.getOffset() : 0;
        int page = offset / Math.max(limit, 1);

        Page<DataProductRecord> pageResult = recordRepository.findByProductId(
                productId, PageRequest.of(page, limit));

        List<Object> records = pageResult.getContent().stream()
                .map(DataProductRecord::getCanonicalData)
                .collect(Collectors.toList());

        return DataProductResponse.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .domain(product.getDomain())
                .format(product.getFormat())
                .records(records)
                .totalCount((int) pageResult.getTotalElements())
                .build();
    }

    /**
     * Lista produtos de dado por dominio.
     */
    public List<DataProduct> list(String domain) {
        if (domain == null || domain.isBlank()) {
            return repository.findAll();
        }
        return repository.findByDomain(domain);
    }

    /**
     * Busca produto por ID.
     */
    public DataProduct findByProductId(String productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produto de dado nao encontrado: " + productId));
    }
}
