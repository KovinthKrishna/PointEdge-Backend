package com.eternalcoders.pointedge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceItemDTO {
    private Long itemId;
    private String productName;
    private Double price;
    private int quantity;
}