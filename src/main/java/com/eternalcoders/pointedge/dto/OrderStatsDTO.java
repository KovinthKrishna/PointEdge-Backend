package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderStatsDTO {
    private long totalOrders;
    private double totalRevenue;
}
