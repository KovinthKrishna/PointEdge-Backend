package com.eternalcoders.pointedge.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReturnRequestDTO {
    private String invoiceNumber;
    private List<ReturnedItemDTO> items;
    private String refundMethod;
    private double totalAmount;
    private String reason;
}