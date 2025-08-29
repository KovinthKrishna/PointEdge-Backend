package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.EmployeeDTO;
import com.eternalcoders.pointedge.dto.EmployeeInfoDTO;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.exception.ResourceNotFoundException;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import com.eternalcoders.pointedge.security.JwtUtil;
import com.eternalcoders.pointedge.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:3000")
public class EmployeeController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmployeeRepository employeeRepository;

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerEmployee(@RequestBody EmployeeDTO dto) {
        employeeService.registerEmployee(dto);
        return ResponseEntity.ok("Registered Successfully");
    }

    @GetMapping("/register/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String email = jwtUtil.extractUsername(token);
            Optional<Employee> employee = employeeRepository.findByEmail(email);
            return employee.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.status(401).body("Unauthorized");
    }

    @PostMapping("/update-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateProfile(@RequestBody EmployeeDTO dto, Principal principal) {
        String email = principal.getName();
        employeeService.updateNameAndAvatar(email, dto.getName(), dto.getAvatar());
        return ResponseEntity.ok("Profile updated successfully");
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@RequestBody EmployeeDTO dto, Principal principal) {
        String email = principal.getName();

        // Validate input passwords from dto
        String currentPassword = dto.getConfirmPassword();  // use confirmPassword field as current password
        String newPassword = dto.getTempPassword();         // use tempPassword field as new password

        if (currentPassword == null || newPassword == null || currentPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Current and new password must be provided");
        }

        employeeService.changePassword(email, currentPassword, newPassword);
        return ResponseEntity.ok("Password changed successfully");
    }



    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = employeeService.getAllEmployees().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        try {
            Employee employee = employeeService.getEmployeeById(id);
            return ResponseEntity.ok(convertToDTO(employee));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        Employee savedEmployee = employeeService.createEmployee(employeeDTO);
        return ResponseEntity.ok(convertToDTO(savedEmployee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO employeeDTO) {
        try {
            employeeDTO.setId(id);
            Employee updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
            return ResponseEntity.ok(convertToDTO(updatedEmployee));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(@RequestParam String query) {
        List<EmployeeDTO> employees = employeeService.searchEmployees(query).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    // Updated conversion methods for single 'name' field
    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setPhoneNumber(employee.getPhoneNumber());
        dto.setRole(employee.getRole());
        dto.setAvatar(employee.getAvatar());
        dto.setStatus(employee.getStatus());
        dto.setLocation(employee.getLocation());
        dto.setName(employee.getName());
        return dto;
    }

    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setId(dto.getId());
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setRole(dto.getRole());
        employee.setAvatar(dto.getAvatar());
        employee.setStatus(dto.getStatus());
        employee.setLocation(dto.getLocation());
        return employee;
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeInfoDTO> getLoggedInEmployee(@AuthenticationPrincipal Object principal) {
        if (principal instanceof String email) {
            Optional<Employee> optionalEmployee = employeeService.findByEmail(email);

            if (optionalEmployee.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Employee employee = optionalEmployee.get();
            EmployeeInfoDTO dto = new EmployeeInfoDTO(
                    employee.getId(),
                    employee.getName(),
                    employee.getRole()
            );

            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}