package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.EmployeeDTO;

public interface EmployeeService {
    void registerEmployee(EmployeeDTO dto);
    void sendResetPasswordToken(String email);
    void resetPassword(String token, String newPassword);
}
