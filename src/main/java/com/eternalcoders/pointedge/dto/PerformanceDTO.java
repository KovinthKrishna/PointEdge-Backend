package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDTO {
    private String id;        // Employee ID
    private String name;      // Employee name
    private String avatar;    // Employee avatar URL
    private String role;      // Employee role
    private Integer orders;   // Number of orders
    private BigDecimal sales; // Total sales amount
    private String workingHours; // Total working hours
}