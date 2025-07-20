package com.eternalcoders.pointedge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceItemDTO {
    private Long itemId;  // This should match what frontend expects
    private String productName;
    private Double price;
    private Integer quantity;

    // Constructor for easy mapping
    public InvoiceItemDTO(Long itemId, String productName, Double price, Integer quantity) {
        this.itemId = itemId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    // Default constructor
    public InvoiceItemDTO() {}
}