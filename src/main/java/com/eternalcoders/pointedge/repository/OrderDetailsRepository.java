package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.OrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDetailsRepository extends JpaRepository<OrderDetails, Long> {
    
    // Basic CRUD methods inherited from JpaRepository
    
    // Find orders by customer
    List<OrderDetails> findByCustomerId(Long customerId);
    
    // Find orders by order ID
    Optional<OrderDetails> findByOrderId(String orderId);
    
    // Find orders by date range
    List<OrderDetails> findByDatetimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find orders by item ID
    List<OrderDetails> findByItemId(Long itemId);
    
    // Find orders by loyalty tier
    List<OrderDetails> findByLoyaltyTier(String loyaltyTier);
    
    // Find orders with discount applied
    @Query("SELECT o FROM OrderDetails o WHERE o.totalDiscount > 0")
    List<OrderDetails> findOrdersWithDiscount();
    
    // Find orders by customer and date range
    List<OrderDetails> findByCustomerIdAndDatetimeBetween(Long customerId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Count orders by customer
    @Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.customer.id = :customerId")
    Long countOrdersByCustomer(@Param("customerId") Long customerId);
    
    // Sum total amount spent by customer
    @Query("SELECT SUM(o.amount) FROM OrderDetails o WHERE o.customer.id = :customerId")
    Double sumTotalAmountByCustomer(@Param("customerId") Long customerId);
    
    // Sum total discount received by customer
    @Query("SELECT SUM(o.totalDiscount) FROM OrderDetails o WHERE o.customer.id = :customerId")
    Double sumTotalDiscountByCustomer(@Param("customerId") Long customerId);
    
    // Count total orders in date range
    @Query("SELECT COUNT(DISTINCT o.orderId) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
    Long countTotalOrdersInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sum of all discounts in date range
    @Query("SELECT SUM(o.totalDiscount) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
    Double sumTotalDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sum of item discounts in date range
    @Query("SELECT SUM(o.itemDiscount) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
    Double sumItemDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sum of category discounts in date range
    @Query("SELECT SUM(o.categoryDiscount) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
    Double sumCategoryDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sum of loyalty discounts in date range
    @Query("SELECT SUM(o.loyaltyDiscount) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
    Double sumLoyaltyDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Loyalty tier specific discounts in date range
    @Query("SELECT SUM(o.loyaltyDiscount) FROM OrderDetails o WHERE o.loyaltyTier = :tier AND o.datetime BETWEEN :startDate AND :endDate")
    Double sumLoyaltyDiscountByTierInDateRange(@Param("tier") String tier, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count of loyalty tier specific discounts in date range
    @Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.loyaltyTier = :tier AND o.datetime BETWEEN :startDate AND :endDate")
    Long countLoyaltyDiscountByTierInDateRange(@Param("tier") String tier, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Top discounted items
    @Query("SELECT o.itemId, SUM(o.itemDiscount) as totalDiscount, COUNT(o) as count FROM OrderDetails o " +
           "WHERE o.datetime BETWEEN :startDate AND :endDate AND o.itemDiscount > 0 " +
           "GROUP BY o.itemId ORDER BY totalDiscount DESC")
    List<Object[]> findTopDiscountedItemsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count discounts by range
    @Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.totalDiscount < :threshold AND o.datetime BETWEEN :startDate AND :endDate")
    Long countDiscountsBelowThreshold(@Param("threshold") Double threshold, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.totalDiscount >= :threshold AND o.datetime BETWEEN :startDate AND :endDate")
    Long countDiscountsAboveOrEqualThreshold(@Param("threshold") Double threshold, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Add these methods to your OrderDetailsRepository interface

// Count orders with item discount in date range
@Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.itemDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate")
Long countOrdersWithItemDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

// Count orders with category discount in date range
@Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.categoryDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate")
Long countOrdersWithCategoryDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

// Count orders with loyalty discount in date range
@Query("SELECT COUNT(o) FROM OrderDetails o WHERE o.loyaltyDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate")
Long countOrdersWithLoyaltyDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

@Query("SELECT c.tier AS tier, COUNT(DISTINCT c.id) AS count " +
       "FROM OrderDetails o JOIN o.customer c " +
       "WHERE o.datetime BETWEEN :startDate AND :endDate " +
       "GROUP BY c.tier")
List<Object[]> countCustomersByTierInDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
@Query("SELECT COUNT(c.id) FROM Customer c")
Long countTotalCustomers();

// Add these methods to OrderDetailsRepository
@Query("SELECT o.loyaltyTier AS tier, COUNT(o) AS count " +
       "FROM OrderDetails o " +
       "WHERE o.loyaltyDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate " +
       "GROUP BY o.loyaltyTier")
List<Object[]> countOrdersWithLoyaltyDiscountByTierInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                              @Param("endDate") LocalDateTime endDate);

@Query("SELECT o.loyaltyTier AS tier, SUM(o.loyaltyDiscount) AS totalDiscount " +
       "FROM OrderDetails o " +
       "WHERE o.loyaltyDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate " +
       "GROUP BY o.loyaltyTier")
List<Object[]> sumLoyaltyDiscountByTierInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);

// Add to OrderDetailsRepository interface
@Query("SELECT o.itemId, SUM(o.amount) as totalAmount, SUM(o.itemDiscount) as totalDiscount, COUNT(o) as count " +
       "FROM OrderDetails o " +
       "WHERE o.itemDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate " +
       "GROUP BY o.itemId " +
       "ORDER BY count DESC")
List<Object[]> findItemDiscountAnalyticsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

@Query("SELECT p.name FROM Product p WHERE p.id = :id")
    Optional<String> findNameById(@Param("id") Long id);

// add category discount methods

@Query("SELECT p.category.id, SUM(o.amount) as totalAmount, SUM(o.categoryDiscount) as totalDiscount, COUNT(o) as count " +
           "FROM OrderDetails o JOIN Product p ON o.itemId = p.id " +
           "WHERE o.categoryDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate " +
           "GROUP BY p.category.id " +
           "ORDER BY count DESC")
    List<Object[]> findCategoryDiscountAnalyticsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.name FROM Category c WHERE c.id = :id")
    Optional<String> findCategoryNameById(@Param("id") Long id);

    
// Add to OrderDetailsRepository interface
@Query("SELECT SUM(o.pointsEarned) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
Double sumPointsEarnedInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

@Query("SELECT SUM(o.amount) FROM OrderDetails o WHERE o.datetime BETWEEN :startDate AND :endDate")
Double sumTotalAmountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

@Query("SELECT SUM(o.amount) FROM OrderDetails o WHERE o.loyaltyDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate")
Double sumAmountWithLoyaltyDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

@Query("SELECT SUM(o.amount) FROM OrderDetails o WHERE o.itemDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate")
Double sumAmountWithItemDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

@Query("SELECT SUM(o.amount) FROM OrderDetails o WHERE o.categoryDiscount > 0 AND o.datetime BETWEEN :startDate AND :endDate")
Double sumAmountWithCategoryDiscountInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
}

