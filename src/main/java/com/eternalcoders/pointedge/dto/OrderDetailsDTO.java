package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long itemId;
    private Long discountId;
    private String itemName;
    private LocalDateTime datetime;
    private Double amount;
    private Double totalDiscount;
    private Double itemDiscount;
    private Double categoryDiscount;
    private Double loyaltyDiscount;
    private String loyaltyTier;
    private Double pointsEarned;
}