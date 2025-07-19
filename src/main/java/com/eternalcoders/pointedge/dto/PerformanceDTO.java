package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDTO {
    private String id;       
    private String name;      
    private String avatar;    
    private String role;      
    private int totalOrders; 
    private double totalSales;
    private String workingHours; 
}