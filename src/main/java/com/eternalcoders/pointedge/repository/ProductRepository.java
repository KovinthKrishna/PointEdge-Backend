package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    void reduceStock(@Param("productId") Long productId, @Param("quantity") long quantity);

    @Query("SELECT p FROM Product p " +
            "JOIN p.brand b " +
            "JOIN p.category c " +
            "WHERE (:brandId IS NULL OR b.id = :brandId) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:hidden IS NULL OR p.hidden = :hidden) " +
            "ORDER BY p.name ASC")
    List<Product> findProductsByFilters(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("hidden") Boolean hidden
    );
}
