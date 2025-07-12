package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByNameContainingIgnoreCase(String name);
    List<Employee> findByStatus(Employee.EmployeeStatus status);

    boolean existsByEmail(String email); // already present

    Optional<Employee> findByEmail(String email); // ✅ Add this line for login

}
