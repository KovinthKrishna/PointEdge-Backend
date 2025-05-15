package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Discount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;

    public LoyaltyTier getLoyaltyType() {
        return this.loyaltyType;
    }    
    
    public enum DiscountType {
        ITEM,
        CATEGORY,
        LOYALTY
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DiscountType type;
    
    @ManyToOne
    @JoinColumn(
        name = "item_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_discount_product"),
        unique = false
    )
    private Product item;  
    
    @ManyToOne
    @JoinColumn(
        name = "category_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_discount_category"),
        unique = false
    )
    private Category category;
    
    public enum LoyaltyTier {
        GOLD,
        SILVER,
        BRONZE,
        NOTLOYALTY
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loyalty_type")
    private LoyaltyTier loyaltyType;
    
    @Column(name = "amount")
    private Double amount;
    
    @Column(name = "percentage")
    private Double percentage;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "duration", nullable = false)
    private String duration;
    
    @AssertTrue(message = "Only one of item, category, or loyaltyType must be set based on type")
    private boolean isValidTarget() {
        return switch (type) {
            case ITEM -> item != null && category == null && loyaltyType == null;
            case CATEGORY -> category != null && item == null && loyaltyType == null;
            case LOYALTY -> loyaltyType != null && item == null && category == null;
        };
    }
    
    @AssertTrue(message = "Only one of amount or percentage can be set")
    private boolean isValidDiscountValue() {
        return (amount == null && percentage != null) ||
                (percentage == null && amount != null);
    }
}