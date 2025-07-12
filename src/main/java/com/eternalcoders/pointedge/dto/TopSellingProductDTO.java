package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopSellingProductDTO {
    private Long productId;
    private String productName;
    private Long totalQuantitySold;
}