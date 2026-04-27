package com.stockroom.repository;

import com.stockroom.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Flexible search — both params are optional.
     * Drives the GET /api/products?search=&category= endpoint.
     * MAX_ITEMS equivalent here is PAGINATION_DEFAULT_SIZE (from ConfigMap).
     */
    @Query("""
       SELECT p FROM Product p
       WHERE (CAST(:search AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
       AND   (CAST(:categoryId AS long) IS NULL OR p.category.id = :categoryId)
       ORDER BY p.createdAt DESC
       """)
    Page<Product> search(@Param("search")     String search,
                         @Param("categoryId") Long   categoryId,
                         Pageable             pageable);
}
