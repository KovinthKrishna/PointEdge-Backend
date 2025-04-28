package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository; // Ensure Spring Data JPA dependency is present

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmail(String email); // for checking duplicates
}
