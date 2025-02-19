package com.eternalcoders.pointedge.dto;

public interface ProductOrderQuantityDTO {
    Long getProductId();

    String getProductName();

    Double getPricePerUnit();

    Long getTotalQuantity();
}
