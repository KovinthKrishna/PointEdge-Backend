package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.ForgotPasswordRequest;
import com.eternalcoders.pointedge.dto.LoginRequest;
import com.eternalcoders.pointedge.dto.LoginResponse;
import com.eternalcoders.pointedge.dto.ResetPasswordRequest;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import com.eternalcoders.pointedge.security.JwtUtil;
import com.eternalcoders.pointedge.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeeService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<Employee> employeeOptional = employeeRepository.findByEmail(loginRequest.getEmail());

        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), employee.getTempPassword())) {
                String role = employee.getRole();
                String token = jwtUtil.generateToken(employee.getEmail(), role);
                return ResponseEntity.ok(new LoginResponse(token, employee.getRole()));
            } else {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
        } else {
            return ResponseEntity.status(401).body("User not found");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userService.sendResetPasswordToken(request.getEmail());
            return ResponseEntity.ok("Reset email sent");
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }



    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return "Password reset successfully";
    }
}
