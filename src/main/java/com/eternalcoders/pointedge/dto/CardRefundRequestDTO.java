package com.eternalcoders.pointedge.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CardRefundRequestDTO {

    @NotBlank
    private String invoiceNumber;

    @NotBlank
    private String refundMethod;

    @NotNull
    private Double totalAmount;

    @NotBlank
    private String accountHolderName;

    @NotBlank
    private String bankName;

    @NotBlank
    private String accountNumber;

    @NotEmpty
    private List<ReturnedItemDTO> items;
}