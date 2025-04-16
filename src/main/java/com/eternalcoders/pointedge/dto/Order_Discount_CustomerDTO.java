package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Order_Discount_CustomerDTO {
    private Long id;
    private Long orderId;
    private Long discountId;
    private Long customerId;
    private LocalDateTime datetime;
    private Double amount;
    private Double itemDiscount;
    private Double categoryDiscount;
    private Double loyaltyDiscount;
}