package com.eternalcoders.pointedge.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class InvoiceDTO {
    private String invoiceNumber;
    private List<InvoiceItemDTO> items;
    private LocalDateTime date;
    private Double totalAmount;
    private double loyaltyPoints;
}
