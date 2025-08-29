package com.eternalcoders.pointedge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceItemDTO {
    private Long itemId;        // This is InvoiceItem.id
    private Long productId;     // Add this - Product.id
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