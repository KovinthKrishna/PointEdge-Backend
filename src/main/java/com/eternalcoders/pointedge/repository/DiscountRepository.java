package com.eternalcoders.pointedge.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eternalcoders.pointedge.dto.CustomerDTO;
import com.eternalcoders.pointedge.entity.Customer;
import com.eternalcoders.pointedge.entity.Discount;
import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.entity.LoyaltyThresholds;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    List<Discount> findByType(DiscountType discountType);
    
    void deleteByType(DiscountType discountType);

    @Query(value = "SELECT name FROM products", nativeQuery = true)
    List<String> findAllProductNames();
    
    @Query(value = "SELECT name FROM categories", nativeQuery = true)
    List<String> findAllCategoryNames();

    @Query("SELECT DISTINCT d.name FROM Discount d")
    List<String> findAllDiscountNames();

    @Query("SELECT COUNT(a) > 0 FROM Admin a WHERE a.password = :password")
    boolean existsByAdminPassword(@Param("password") String password);

    @Modifying
    @Query("DELETE FROM Discount d WHERE d.type = :type")
    void deleteByTypeWithAuth(@Param("type") DiscountType type);

    @Query("SELECT COUNT(d) FROM Discount d WHERE d.type = :type")
    long countByType(@Param("type") DiscountType type);

    @Query("SELECT lt FROM LoyaltyThresholds lt WHERE lt.id = 1")
    Optional<LoyaltyThresholds> findLoyaltyThresholds();
    
    @Modifying
    @Query("UPDATE LoyaltyThresholds lt SET lt.gold = :gold, lt.silver = :silver, lt.bronze = :bronze, lt.points = :points WHERE lt.id = 1")
    void updateLoyaltyThresholds(
        @Param("gold") double gold,
        @Param("silver") double silver,
        @Param("bronze") double bronze,
        @Param("points") double points
    );

    // below methods for integration

    @Query("SELECT d FROM Discount d WHERE " +
           "d.item.id = :itemId AND " +
           "d.isActive = true AND " +
           "(:loyaltyTier IS NULL OR d.loyaltyType = :loyaltyTier)")
    List<Discount> findActiveItemDiscounts(
        @Param("itemId") Long itemId,
        @Param("loyaltyTier") Discount.LoyaltyTier loyaltyTier);

    @Query("SELECT d FROM Discount d WHERE " +
           "d.category.id = :categoryId AND " +
           "d.isActive = true AND " +
           "(:loyaltyTier IS NULL OR d.loyaltyType = :loyaltyTier)")
    List<Discount> findActiveCategoryDiscounts(
        @Param("categoryId") Long categoryId,
        @Param("loyaltyTier") Discount.LoyaltyTier loyaltyTier);

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

    // get customer loyalty type by id
    // @Query("SELECT c.loyaltyType FROM Customer c WHERE c.id = :customerId")
    // Optional<Discount.LoyaltyTier> findCustomerLoyaltyTierById(@Param("customerId") Long customerId);

    // get customer id by phone
    @Query(value = "SELECT id FROM customers WHERE phone = :customerId", nativeQuery = true)
    Optional<Long> findCustomerIdByPhone(@Param("customerId") String customerId);

    // get all applicable loyslty discounts for a given product ID and customer phone number
    @Query("SELECT d FROM Discount d WHERE " +
       "d.type = 'LOYALTY' AND " +
       "d.isActive = true AND " +
       "d.loyaltyType = :tier")
    List<Discount> findActiveLoyaltyDiscountsByTypeAndTier(@Param("tier") Discount.LoyaltyTier tier);
 
    // get all applicable category discounts for a given product ID and customer phone number
    
    // get all applicable discounts for a given product ID and customer phone number

    

    // find items for given category
    @Query("SELECT p.id, p.category.id FROM Product p WHERE p.id IN :itemIds")
    List<Object[]> findCategoryIdsByItemIds(@Param("itemIds") Collection<Long> itemIds);

    // find price for given item
    @Query("SELECT p.price FROM Product p WHERE p.id = :itemId")
    Optional<BigDecimal> findPriceByItemId(@Param("itemId") Long itemId);

    // final return with discount amout and customer info
    @Query("SELECT c FROM Customer c WHERE c.phone = :phone")
    Optional<Customer> findCustomerByPhone(@Param("phone") String phone);
}