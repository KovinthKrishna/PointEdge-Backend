package com.eternalcoders.pointedge.dto;

import com.eternalcoders.pointedge.entity.Employee.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String name;
    private String role;
    private String avatar;
    private String location;
    private EmployeeStatus status;
}