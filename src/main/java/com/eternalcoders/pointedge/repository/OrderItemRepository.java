package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi.product.id AS productId, " +
            "oi.product.name AS productName, " +
            "oi.pricePerUnit AS pricePerUnit, " +
            "SUM(oi.quantity) AS totalQuantity, " +
            "oi.product.imageName AS imageName " +
            "FROM OrderItem oi " +
            "WHERE (:brandId IS NULL OR oi.product.brand.id = :brandId) " +
            "AND (:categoryId IS NULL OR oi.product.category.id = :categoryId) " +
            "AND (:startDate IS NULL OR oi.order.orderDate BETWEEN :startDate AND :endDate) " +
            "AND (:search IS NULL OR LOWER(oi.product.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY oi.product.id, oi.product.name, oi.pricePerUnit, oi.product.imageName " +
            "ORDER BY oi.product.name ASC, MAX(oi.order.orderDate) DESC")
    Page<ProductOrderQuantityDTO> getTotalOrdersForProducts(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT new com.eternalcoders.pointedge.dto.TopSellingProductDTO(
            oi.product.id, oi.product.name, SUM(oi.quantity)
        )
        FROM OrderItem oi
        GROUP BY oi.product.id, oi.product.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<TopSellingProductDTO> findTopSellingProducts();

    @Query("""
        SELECT new com.eternalcoders.pointedge.dto.DailySalesDTO(
            o.orderDate, SUM(CAST(oi.pricePerUnit * oi.quantity AS double))
        )
        FROM OrderItem oi
        JOIN oi.order o
        GROUP BY o.orderDate
        ORDER BY o.orderDate
    """)
    List<DailySalesDTO> getDailySales();

    @Query("""
        SELECT new com.eternalcoders.pointedge.dto.ProductRevenueDTO(
            oi.product.id, oi.product.name, SUM(CAST(oi.pricePerUnit * oi.quantity AS double))
        )
        FROM OrderItem oi
        GROUP BY oi.product.id, oi.product.name
        ORDER BY SUM(oi.pricePerUnit * oi.quantity) DESC
    """)
    List<ProductRevenueDTO> getRevenueByProduct();

    @Query("""
        SELECT new com.eternalcoders.pointedge.dto.CategoryDistributionDTO(
            p.category.name, COUNT(p.id)
        )
        FROM Product p
        GROUP BY p.category.name
    """)
    List<CategoryDistributionDTO> getCategoryDistribution();
}
