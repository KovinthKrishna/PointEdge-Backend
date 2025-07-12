package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryDistributionDTO {
    private String categoryName;
    private Long productCount;
}