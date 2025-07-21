package com.eternalcoders.pointedge.dto;

import lombok.Data;

import java.util.List;

@Data
public class RefundRequestDTO {
    private String invoiceNumber;
    private String refundMethod;
    private List<ReturnedItemDTO> items;
}