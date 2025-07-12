package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.CategoryDistributionDTO;
import com.eternalcoders.pointedge.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    void reduceStock(@Param("productId") Long productId, @Param("quantity") long quantity);

    @Query("SELECT p FROM Product p " +
            "WHERE (:brandId IS NULL OR p.brand.id = :brandId) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:hidden IS NULL OR p.hidden = :hidden) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findFilteredProducts(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("hidden") Boolean hidden,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<Product> findByBarcode(String barcode);

    @Query("""
    SELECT new com.eternalcoders.pointedge.dto.CategoryDistributionDTO(
        p.category.name, COUNT(p.id)
    )
    FROM Product p
    WHERE p.hidden = false
    GROUP BY p.category.name
""")
    List<CategoryDistributionDTO> getProductCategoryDistribution();
}
