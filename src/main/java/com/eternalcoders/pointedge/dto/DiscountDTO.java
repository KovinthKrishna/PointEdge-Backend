package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.eternalcoders.pointedge.entity.Discount.DiscountType;
import com.eternalcoders.pointedge.entity.Discount.LoyaltyTier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountDTO {
    
    public Long id;
    
    private String name;
    
    private DiscountType type;
    
    private Long itemId;     // Only the ID to avoid heavy object transfer
    private Long categoryId; // Same here
    
    private LoyaltyTier loyaltyType;
    
    private Double amount;
    private Double percentage;
    
    private LocalDateTime startDate;
    
    private Boolean isActive ;
    
    private String duration;

    // public Object getId() {
    //     return id;
    // }
    
    public Object getPercentage() {
        return percentage;
    }
    
    public Object getAmount() {
        return amount;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    
}