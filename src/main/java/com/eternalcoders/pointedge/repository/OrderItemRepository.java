package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi.product.id AS productId, oi.product.name AS productName, " +
            "oi.pricePerUnit AS pricePerUnit, SUM(oi.quantity) AS totalQuantity " +
            "FROM OrderItem oi " +
            "JOIN oi.product p " +
            "JOIN p.brand b " +
            "JOIN p.category c " +
            "WHERE (:brandId IS NULL OR b.id = :brandId) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:startDate IS NULL OR oi.order.orderDate BETWEEN :startDate AND :endDate) " +
            "GROUP BY oi.product.id, oi.product.name, oi.pricePerUnit " +
            "ORDER BY p.name ASC, MAX(oi.order.orderDate) DESC")
    List<ProductOrderQuantityDTO> getProductOrderQuantities(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
