package com.eternalcoders.pointedge.dto;

import lombok.Data;

@Data
public class ReturnedItemDTO {
    private Long itemId;
    private Long invoiceItemId;
    private int quantity;
    private double unitPrice;
    private double refundAmount;
    private String reason;
    private String photoPath;
    private String productName;
}
