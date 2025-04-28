package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.eternalcoders.pointedge.entity.Customer.Title;
import com.eternalcoders.pointedge.entity.Customer.Tier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    
    private Long id;
    
    private String name;
    
    private Title title;
    
    private String email;
    
    private String phone;
    
    private Double points;
    
    private Tier tier;
}