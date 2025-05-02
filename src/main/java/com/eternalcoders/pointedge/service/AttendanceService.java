package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceService {
    List<Attendance> getAllAttendances();
    Optional<Attendance> getAttendanceById(Long id);
    Attendance saveAttendance(Attendance attendance);
    void deleteAttendance(Long id);
    List<Attendance> findByEmployee(Employee employee);
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date);
    Attendance clockIn(Long employeeId, LocalTime time);
    Attendance clockOut(Long employeeId, LocalTime time);
    String calculateTotalHours(LocalTime clockIn, LocalTime clockOut);
    String calculateOTHours(LocalTime clockIn, LocalTime clockOut, LocalTime standardEnd);
}