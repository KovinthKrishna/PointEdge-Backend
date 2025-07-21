package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftReportDTO {
    // Employee basic info
    private Long employeeId;
    private String employeeName;
    private String role;
    
    // Individual shift data
    private LocalDate shiftDate;
    private LocalTime clockIn;
    private LocalTime clockOut;
    private String otHours;
    private String workingHours;
    private String shiftType;
    
    // Employee performance totals (same for all shifts of this employee)
    private Integer totalOrders;
    private Double totalSales;
    private String totalWorkingHours;

    
}