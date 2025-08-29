package com.eternalcoders.pointedge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnedItemDTO {
    @NotNull(message = "Item ID cannot be null")
    private Long itemId;

    private Long invoiceItemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private double unitPrice;

    @DecimalMin(value = "0.0", message = "Refund amount cannot be negative")
    private double refundAmount;

    private String reason;
    private String photoPath;
    private String productName;
}