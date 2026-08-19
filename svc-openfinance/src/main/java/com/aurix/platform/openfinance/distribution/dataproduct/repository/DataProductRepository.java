package com.aurix.platform.openfinance.distribution.dataproduct.repository;

import com.aurix.platform.openfinance.distribution.dataproduct.entity.DataProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository de produtos de dado.
 */
@Repository
public interface DataProductRepository extends JpaRepository<DataProduct, Long> {

    Optional<DataProduct> findByProductId(String productId);

    List<DataProduct> findByDomain(String domain);

    List<DataProduct> findByStatus(String status);
}
