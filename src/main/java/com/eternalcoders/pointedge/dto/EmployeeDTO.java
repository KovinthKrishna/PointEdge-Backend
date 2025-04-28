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
}