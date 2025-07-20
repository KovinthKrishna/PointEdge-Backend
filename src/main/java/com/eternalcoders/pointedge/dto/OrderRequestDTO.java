package com.eternalcoders.pointedge.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDTO {
    private String customerName;
    private String customerPhone;
    private int loyaltyPoints;
    private String discountCode;
    private double amount;
    private double totalDiscount;
    private double total;
    private Long employeeId;
    private String cashierName;
    private List<OrderItemDTO> items;
}

