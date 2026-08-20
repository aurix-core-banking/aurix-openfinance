package com.aurix.platform.openfinance.distribution.dataproduct.service;

import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductResponse;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.QueryParams;
import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRecordRepository;
import com.aurix.platform.openfinance.distribution.dataproduct.repository.DataProductRepository;
import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que materialize()/serve() persistem e consultam dados reais em
 * data_product_records — antes disso materialize() só logava e serve()
 * sempre devolvia lista vazia com totalCount=0.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataProductServiceTest {

    @Autowired
    private DataProductService dataProductService;

    @Autowired
    private DataProductRepository dataProductRepository;

    @Autowired
    private DataProductRecordRepository dataProductRecordRepository;

    @BeforeEach
    void setUp() {
        dataProductRecordRepository.deleteAll();
        dataProductRepository.deleteAll();
    }

    @Test
    void deveMaterializarEServirRegistrosReais() {
        DataProduct product = dataProductService.create(new DataProductRequest(
                "Contas Correntes", "Produto de contas", "accounts", "CONTA",
                "JSON", "{}", "/products/accounts"));

        CanonicalRecord record = CanonicalRecord.criar(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                ResourceType.CONTA, "{\"conta\":\"1234\"}", "1.0", "checksum-fake");

        dataProductService.materialize(product, List.of(record));

        DataProductResponse response = dataProductService.serve(
                product.getProductId(), new QueryParams(null, 10, 0));

        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getRecords().size());
        assertTrue(response.getRecords().get(0).toString().contains("1234"));
    }
}
