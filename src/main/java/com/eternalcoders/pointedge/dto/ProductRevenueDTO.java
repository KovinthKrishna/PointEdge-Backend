package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductRevenueDTO {
    private Long productId;
    private String productName;
    private Double revenue;
}