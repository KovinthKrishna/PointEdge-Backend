package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiscountCalculatorRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByItemAndIsActiveAndStartDateBefore(Product item, Boolean isActive, LocalDateTime date);
    List<Discount> findByCategoryAndIsActiveAndStartDateBefore(Category category, Boolean isActive, LocalDateTime date);
    List<Discount> findByLoyaltyTypeAndIsActiveAndStartDateBefore(Discount.LoyaltyTier loyaltyType, Boolean isActive, LocalDateTime date);
    Discount findByName(String name);
}
