package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ProductReturnRateDTO {
    private Long productId;
    private String productName;
    private Double returnRate;

    public ProductReturnRateDTO(Long productId, String productName, Double returnRate) {
        this.productId = productId;
        this.productName = productName;
        this.returnRate = returnRate;
    }
}