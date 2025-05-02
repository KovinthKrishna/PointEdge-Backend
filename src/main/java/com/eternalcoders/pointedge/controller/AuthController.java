package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.LoginRequest;
import com.eternalcoders.pointedge.dto.LoginResponse;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import com.eternalcoders.pointedge.security.JwtUtil;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<Employee> employeeOptional = employeeRepository.findByEmail(loginRequest.getEmail());

        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), employee.getTempPassword())) {
                String token = jwtUtil.generateToken(employee.getEmail());
                return ResponseEntity.ok(new LoginResponse(token));
            } else {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
        } else {
            return ResponseEntity.status(401).body("User not found");
        }
    }
}
