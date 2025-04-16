package com.eternalcoders.pointedge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}