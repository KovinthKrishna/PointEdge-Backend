package com.eternalcoders.pointedge.dto;

import com.eternalcoders.pointedge.enums.RequestStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RefundRequestDetailsDTO {

    private Long requestId;

    private String invoiceNumber;

    private String refundMethod;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private double totalRefundAmount;

    private RequestStatus status;

    private Long customerId;

    private String customerName; // Optional, for UI display

    private String reviewedBy; // Optional, if you store reviewer info

    private List<ReturnedItemDTO> items;

}