package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployee(Employee employee);
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date);
    List<Attendance> findByEmployeeAndDateBetween(Employee employee, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByDateBetween(LocalDate startDate, LocalDate endDate);
}