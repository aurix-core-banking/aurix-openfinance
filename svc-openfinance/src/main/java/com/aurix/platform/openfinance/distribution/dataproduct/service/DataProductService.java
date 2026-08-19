package com.aurix.platform.openfinance.distribution.dataproduct.service;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProductStatus;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductResponse;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.QueryParams;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servico de gestao de produtos de dado.
 * Responsavel por criar, materializar e servir produtos de dado.
 */
@Service
public class DataProductService {

    private static final Logger log = LoggerFactory.getLogger(DataProductService.class);

    private final DataProductRepository repository;

    public DataProductService(DataProductRepository repository) {
        this.repository = repository;
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
     * Materializa dados no storage (ClickHouse/MinIO).
     */
    public void materialize(DataProduct product, List<CanonicalRecord> records) {
        log.info("Materializando {} registros para produto {}",
                records.size(), product.getProductId());

        // Implementacao concreta: gravar no ClickHouse ou MinIO
        // por agora, apenas logamos a operacao
        product.activate();
        repository.save(product);
    }

    /**
     * Serve dados de um produto de dado.
     */
    public DataProductResponse serve(String productId, QueryParams params) {
        DataProduct product = repository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produto de dado nao encontrado: " + productId));

        if (!product.isActive()) {
            throw new IllegalStateException(
                    "Produto de dado nao esta ativo: " + productId);
        }

        // Implementacao concreta: consultar ClickHouse/MinIO com filtros
        return DataProductResponse.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .domain(product.getDomain())
                .format(product.getFormat())
                .records(List.of())
                .totalCount(0)
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
