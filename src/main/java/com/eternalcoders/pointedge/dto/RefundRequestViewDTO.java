package com.eternalcoders.pointedge.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RefundRequestViewDTO {
    private Long id;
    private String invoiceNumber;
    private Long invoiceId;
    private Long customerId;
    private String refundMethod;
    private double totalRefundAmount;
    private LocalDateTime createdAt;
    private List<ReturnedItemDTO> items;
}