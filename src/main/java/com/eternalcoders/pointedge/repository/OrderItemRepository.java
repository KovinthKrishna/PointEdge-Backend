package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

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
}
