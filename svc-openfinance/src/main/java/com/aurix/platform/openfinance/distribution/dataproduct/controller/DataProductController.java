package com.aurix.platform.openfinance.distribution.dataproduct.controller;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductRequest;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.DataProductResponse;
import com.aurix.platform.openfinance.distribution.dataproduct.dto.QueryParams;
import com.aurix.platform.openfinance.distribution.dataproduct.service.DataProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller de produtos de dado.
 */
@RestController
@RequestMapping("/api/v1/products")
public class DataProductController {

    private final DataProductService service;

    public DataProductController(DataProductService service) {
        this.service = service;
    }

    /**
     * Cria um novo produto de dado.
     */
    @PostMapping
    public ResponseEntity<DataProduct> create(@RequestBody DataProductRequest request) {
        DataProduct product = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Lista produtos de dado por dominio.
     */
    @GetMapping
    public ResponseEntity<List<DataProduct>> list(
            @RequestParam(required = false) String domain) {
        List<DataProduct> products = service.list(domain);
        return ResponseEntity.ok(products);
    }

    /**
     * Serve dados de um produto de dado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataProductResponse> serve(
            @PathVariable String id,
            @RequestParam(required = false) String filters,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        QueryParams params = new QueryParams(filters, limit, offset);
        DataProductResponse response = service.serve(id, params);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna o schema de um produto de dado.
     */
    @GetMapping("/{id}/schema")
    public ResponseEntity<String> getSchema(@PathVariable String id) {
        DataProduct product = service.findByProductId(id);
        return ResponseEntity.ok(product.getSchema());
    }
}
