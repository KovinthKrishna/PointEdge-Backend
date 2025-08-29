package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeInfoDTO {
    private Long id;
    private String name;
    private String role;
}