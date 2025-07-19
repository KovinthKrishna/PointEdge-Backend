package com.eternalcoders.pointedge.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private int quantity;
    private double pricePerUnit;
}

