package com.eternalcoders.pointedge.dto;

public interface ProductOrderQuantityDTO {
    Long getProductId();

    String getProductName();

    double getPricePerUnit();

    long getTotalQuantity();
}
