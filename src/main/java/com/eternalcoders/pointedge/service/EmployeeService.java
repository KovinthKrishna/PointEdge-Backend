package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.EmployeeDTO;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.exception.ResourceNotFoundException;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerEmployee(EmployeeDTO dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        Employee employee = new Employee();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setTempPassword(passwordEncoder.encode(dto.getTempPassword()));
        employee.setRole(dto.getRole());
        employee.setAvatar(dto.getAvatar());
        employee.setStatus(dto.getStatus());
        employee.setName(dto.getFirstName() + " " + dto.getLastName());

        employeeRepository.save(employee);
    }

    public void sendResetPasswordToken(String email) {
        // Logic for generating and sending password reset token
    }

    public void resetPassword(String token, String newPassword) {
        // Logic for resetting the password using token
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    public Employee createEmployee(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setPhoneNumber(employeeDTO.getPhoneNumber());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());
        employee.setStatus(employeeDTO.getStatus());
        employee.setAvatar(employeeDTO.getAvatar());
        employee.setName(employeeDTO.getFirstName() + " " + employeeDTO.getLastName());
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, EmployeeDTO employeeDTO) {
        Employee employee = getEmployeeById(id);
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setPhoneNumber(employeeDTO.getPhoneNumber());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());
        employee.setStatus(employeeDTO.getStatus());
        employee.setAvatar(employeeDTO.getAvatar());
        employee.setName(employeeDTO.getFirstName() + " " + employeeDTO.getLastName());
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
    }

    public List<Employee> searchEmployees(String query) {
        return employeeRepository.findByNameContainingIgnoreCase(query);
    }

    public List<Employee> findByStatus(Employee.EmployeeStatus status) {
        return employeeRepository.findByStatus(status);
    }

    public void updateNameAndAvatar(String email, String name, String avatar) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (name != null && !name.isBlank()) {
            employee.setName(name);
        }

        if (avatar != null && !avatar.isBlank()) {
            employee.setAvatar(avatar);
        }

        if ((name != null && !name.isBlank()) || (avatar != null && !avatar.isBlank())) {
            employeeRepository.save(employee);
        }
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!passwordEncoder.matches(currentPassword, employee.getTempPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        employee.setTempPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
    }
}