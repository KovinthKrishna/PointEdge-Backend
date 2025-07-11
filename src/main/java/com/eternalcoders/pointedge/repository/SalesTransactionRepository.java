package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.entity.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {
    List<SalesTransaction> findByEmployee(Employee employee);
    
    List<SalesTransaction> findByTransactionDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<SalesTransaction> findByEmployeeAndTransactionDateTimeBetween(
            Employee employee, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(DISTINCT s.orderId) FROM SalesTransaction s WHERE s.employee = :employee AND s.transactionDateTime BETWEEN :startDate AND :endDate")
    Integer countOrdersByEmployeeAndDateRange(Employee employee, LocalDateTime startDate, LocalDateTime endDate);
}