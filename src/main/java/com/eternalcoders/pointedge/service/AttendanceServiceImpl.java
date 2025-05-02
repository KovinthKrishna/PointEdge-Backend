package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    @Override
    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }

    @Override
    public Attendance saveAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    @Override
    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }

    @Override
    public List<Attendance> findByEmployee(Employee employee) {
        return attendanceRepository.findByEmployee(employee);
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    @Override
    public List<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date) {
        return attendanceRepository.findByEmployeeAndDate(employee, date);
    }

    @Override
    public Attendance clockIn(Long employeeId, LocalTime time) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(LocalDate.now());
        attendance.setClockIn(time);
        
        return attendanceRepository.save(attendance);
    }

    @Override
    public Attendance clockOut(Long employeeId, LocalTime time) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        List<Attendance> todayAttendances = attendanceRepository.findByEmployeeAndDate(employee, LocalDate.now());
        if (todayAttendances.isEmpty()) {
            throw new RuntimeException("No clock-in record found for today");
        }
        
        Attendance attendance = todayAttendances.get(todayAttendances.size() - 1);
        attendance.setClockOut(time);
        attendance.setTotalHours(calculateTotalHours(attendance.getClockIn(), time));
        attendance.setOtHours(calculateOTHours(attendance.getClockIn(), time, LocalTime.of(16, 0)));
        
        return attendanceRepository.save(attendance);
    }

    @Override
    public String calculateTotalHours(LocalTime clockIn, LocalTime clockOut) {
        if (clockIn == null || clockOut == null) {
            return "0:00:00";
        }
        
        Duration duration;
        if (clockOut.isBefore(clockIn)) {
            // Handle overnight shifts
            Duration firstPart = Duration.between(clockIn, LocalTime.MAX);
            Duration secondPart = Duration.between(LocalTime.MIN, clockOut);
            duration = firstPart.plus(secondPart).plusSeconds(1); // Add 1 second for midnight
        } else {
            duration = Duration.between(clockIn, clockOut);
        }
        
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public String calculateOTHours(LocalTime clockIn, LocalTime clockOut, LocalTime standardEnd) {
        if (clockIn == null || clockOut == null || clockOut.isBefore(standardEnd)) {
            return "0:00:00";
        }
        
        Duration duration = Duration.between(standardEnd, clockOut);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
}