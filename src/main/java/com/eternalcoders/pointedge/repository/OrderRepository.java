package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.OrderStatsDTO;
import com.eternalcoders.pointedge.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            SELECT new com.eternalcoders.pointedge.dto.OrderStatsDTO(
                COUNT(DISTINCT o.id),
                COALESCE(SUM(oi.pricePerUnit * oi.quantity), 0)
            )
            FROM Order o
            JOIN o.orderItems oi
            JOIN oi.product p
            WHERE (:brandId IS NULL OR p.brand.id = :brandId)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:startDateTime IS NULL OR o.orderDate >= :startDateTime)
              AND (:endDateTime IS NULL OR o.orderDate < :endDateTime)
            """)
    OrderStatsDTO findOrderStats(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * Find all orders by employee ID
     */
    List<Order> findByEmployeeId(Long employeeId);

    /**
     * Find orders by employee ID within date range
     */
    List<Order> findByEmployeeIdAndOrderDateBetween(Long employeeId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find orders by employee ID ordered by date (most recent first)
     */
    List<Order> findByEmployeeIdOrderByOrderDateDesc(Long employeeId);

    /**
     * Find orders within date range for all employees
     */
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get total sales amount for a specific employee (all time)
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.employeeId = :employeeId")
    Double getTotalSalesByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Get total sales amount for a specific employee within date range
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.employeeId = :employeeId AND o.orderDate BETWEEN :startDate AND :endDate")
    Double getTotalSalesByEmployeeIdAndDateRange(@Param("employeeId") Long employeeId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Count total orders for a specific employee (all time)
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.employeeId = :employeeId")
    Long countOrdersByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Count total orders for a specific employee within date range
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.employeeId = :employeeId AND o.orderDate BETWEEN :startDate AND :endDate")
    Long countOrdersByEmployeeIdAndDateRange(@Param("employeeId") Long employeeId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Get all unique employee IDs who have orders
     */
    @Query("SELECT DISTINCT o.employeeId FROM Order o WHERE o.employeeId IS NOT NULL")
    List<Long> findDistinctEmployeeIds();

    /**
     * Find orders by customer name (for search functionality)
     */
    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Find orders by cashier name (for search functionality)
     */
    List<Order> findByCashierNameContainingIgnoreCase(String cashierName);

    /**
     * Get recent orders for an employee (limited)
     */
    List<Order> findTop10ByEmployeeIdOrderByOrderDateDesc(Long employeeId);
}