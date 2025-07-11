package com.eternalcoders.pointedge.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Customer.Tier;
import com.eternalcoders.pointedge.entity.Discount;
import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    List<Discount> findByType(DiscountType discountType);
    
    void deleteByType(DiscountType discountType);

    // get all product names
    @Query(value = "SELECT name FROM products", nativeQuery = true)
    List<String> findAllProductNames();
    
    // get all category names
    @Query(value = "SELECT name FROM categories", nativeQuery = true)
    List<String> findAllCategoryNames();

    // get all discount names
    @Query("SELECT DISTINCT d.name FROM Discount d")
    List<String> findAllDiscountNames();

    // check admin password
    @Query("SELECT COUNT(a) > 0 FROM Admin a WHERE a.password = :password")
    boolean existsByAdminPassword(@Param("password") String password);

    // delete discount by type 
    @Modifying
    @Query("DELETE FROM Discount d WHERE d.type = :type")
    void deleteByTypeWithAuth(@Param("type") DiscountType type);

    // get count of discounts by type
    @Query("SELECT COUNT(d) FROM Discount d WHERE d.type = :type")
    long countByType(@Param("type") DiscountType type);

    // get loyalty thresholds
    @Query("SELECT lt FROM LoyaltyThresholds lt WHERE lt.id = 1")
    Optional<LoyaltyThresholds> findLoyaltyThresholds();
    
    // update loyalty thresholds
    @Modifying
    @Query("UPDATE LoyaltyThresholds lt SET lt.gold = :gold, lt.silver = :silver, lt.bronze = :bronze, lt.points = :points WHERE lt.id = 1")
    void updateLoyaltyThresholds(
        @Param("gold") double gold,
        @Param("silver") double silver,
        @Param("bronze") double bronze,
        @Param("points") double points
    );

    // get all applicable discounts for a given product ID and customer phone number
    @Query("SELECT d FROM Discount d WHERE " +
           "d.item.id = :itemId AND " +
           "d.isActive = true AND " +
           "(:loyaltyTier IS NULL OR d.loyaltyType = :loyaltyTier)")
    List<Discount> findActiveItemDiscounts(
        @Param("itemId") Long itemId,
        @Param("loyaltyTier") Discount.LoyaltyTier loyaltyTier);

    // get all applicable discounts for a given category ID and customer phone number
    @Query("SELECT d FROM Discount d WHERE " +
           "d.category.id = :categoryId AND " +
           "d.isActive = true AND " +
           "(:loyaltyTier IS NULL OR d.loyaltyType = :loyaltyTier)")
    List<Discount> findActiveCategoryDiscounts(
        @Param("categoryId") Long categoryId,
        @Param("loyaltyTier") Discount.LoyaltyTier loyaltyTier);

    // get all applicable discounts for a given product ID and customer phone number
    @Query("SELECT d FROM Discount d WHERE " +
           "d.loyaltyType = :tier AND " +
           "d.isActive = true")
    List<Discount> findActiveLoyaltyDiscounts(
        @Param("tier") Discount.LoyaltyTier tier);

    // get price of an item by id
    @Query("SELECT p.price FROM Product p WHERE p.id = :itemId")
    Optional<Double> findProductPriceById(@Param("itemId") Long itemId);

    // get category id by itemid
    @Query("SELECT p.category.id FROM Product p WHERE p.id = :itemId")
    Optional<Long> findCategoryIdByProductId(@Param("itemId") Long itemId);

    // get customer loyalty type by phone
    @Query(value = "SELECT tier FROM customers WHERE phone = :phoneNumber", nativeQuery = true)
    Optional<Discount.LoyaltyTier> findCustomerLoyaltyTierByPhone(@Param("phoneNumber") String phoneNumber);

    // get customer id by phone
    @Query(value = "SELECT id FROM customers WHERE phone = :customerId", nativeQuery = true)
    Optional<Long> findCustomerIdByPhone(@Param("customerId") String customerId);

    // get all applicable loyslty discounts for a given product ID and customer phone number
    @Query("SELECT d FROM Discount d WHERE " +
       "d.type = 'LOYALTY' AND " +
       "d.isActive = true AND " +
       "d.loyaltyType = :tier")
    List<Discount> findActiveLoyaltyDiscountsByTypeAndTier(@Param("tier") Discount.LoyaltyTier tier);
 
    // find items for given category
    @Query("SELECT p.id, p.category.id FROM Product p WHERE p.id IN :itemIds")
    List<Object[]> findCategoryIdsByItemIds(@Param("itemIds") Collection<Long> itemIds);

    // find price for given item
    @Query("SELECT p.price FROM Product p WHERE p.id = :itemId")
    Optional<BigDecimal> findPriceByItemId(@Param("itemId") Long itemId);

    // final return with discount amout and customer info
    @Query("SELECT c FROM Customer c WHERE c.phone = :phone")
    Optional<Customer> findCustomerByPhone(@Param("phone") String phone);

    // get customer points by phone number
    @Query("SELECT c.points FROM Customer c WHERE c.phone = :phone")
    Optional<Double> findCustomerPointsByPhone(@Param("phone") String phone);

    // update customer points by phone number
    @Modifying
    @Query("UPDATE Customer c SET c.points = :points WHERE c.phone = :phone")
    void updateCustomerPoints(@Param("phone") String phone, @Param("points") Double points);

    // update orderdetails table
    @Query("SELECT p.name FROM Product p WHERE p.id = :itemId")
        Optional<String> findProductNameById(@Param("itemId") Long itemId);

    //update orderdetails table
    @Modifying
    @Query(value = "INSERT INTO order_details (" +
        "order_id, customer_id, item_id, discount_id, datetime, amount, " +
        "total_discount, item_discount, category_discount, " +
        "loyalty_discount, loyalty_tier, points_earned) " +
        "VALUES (:orderId, :customerId, :itemId, :discountId, :datetime, :amount, " +
        ":totalDiscount, :itemDiscount, :categoryDiscount, " +
        ":loyaltyDiscount, :loyaltyTier, :pointsEarned)", nativeQuery = true)
    void saveOrderDetails(
        @Param("orderId") String orderId,
        @Param("customerId") Long customerId,
        @Param("itemId") Long itemId,
        @Param("discountId") Long discountId,
        @Param("datetime") LocalDateTime datetime,
        @Param("amount") Double amount,
        @Param("totalDiscount") Double totalDiscount,
        @Param("itemDiscount") Double itemDiscount,
        @Param("categoryDiscount") Double categoryDiscount,
        @Param("loyaltyDiscount") Double loyaltyDiscount,
        @Param("loyaltyTier") String loyaltyTier,
        @Param("pointsEarned") Double pointsEarned);

    // update customer tier by phone number
    @Modifying
    @Query("UPDATE Customer c SET c.tier = :tier WHERE c.phone = :phone")
    void updateCustomerTier(@Param("phone") String phone, @Param("tier") Tier tier);

}