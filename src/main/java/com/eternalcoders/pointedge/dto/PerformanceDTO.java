package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceDTO {
    private String id;       
    private String name;      
    private String avatar;    
    private String role;      
    private Integer orders;  
    private BigDecimal sales; 
    private String workingHours; 
}