package com.eternalcoders.pointedge.dto;

import lombok.Data;

@Data
public class EmployeeDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String tempPassword;
    private String confirmPassword;
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
    private EmployeeStatus status;
}