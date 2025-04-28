package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.EmployeeDTO;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository repository;

    @Override
    public void registerEmployee(EmployeeDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (!dto.getTempPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        Employee employee = new Employee();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setEmail(dto.getEmail());
        employee.setTempPassword(dto.getTempPassword());

        repository.save(employee);
    }
}
