package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.EmployeeDTO;
import com.eternalcoders.pointedge.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000") // allow frontend access
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody EmployeeDTO dto) {
        employeeService.registerEmployee(dto);
        return ResponseEntity.ok("Registered Successfully");
    }
}
