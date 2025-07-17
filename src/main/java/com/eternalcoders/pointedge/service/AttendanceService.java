package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.exception.ResourceNotFoundException;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }

    public Attendance saveAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }
    
    // Bulk delete method
    public void deleteAttendances(List<Long> ids) {
        attendanceRepository.deleteAllById(ids);
    }

    public List<Attendance> findByEmployeeAndDateBetween(Employee employee, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByEmployeeAndDateBetween(employee, startDate, endDate);
    }

    public List<Attendance> findByEmployee(Employee employee) {
        return attendanceRepository.findByEmployee(employee);
    }

    public List<Attendance> findByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }
    
    public List<Attendance> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByDateBetween(startDate, endDate);
    }

    public List<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date) {
        return attendanceRepository.findByEmployeeAndDate(employee, date);
    }

    // Clock-in logic
    public Attendance clockIn(Long employeeId, LocalTime time) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        // Check if employee already clocked in today but not clocked out
        List<Attendance> todayAttendances = attendanceRepository.findByEmployeeAndDate(employee, LocalDate.now());
        for (Attendance existing : todayAttendances) {
            if (existing.getClockOut() == null) {
                throw new IllegalStateException("Employee already clocked in but not clocked out");
            }
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(LocalDate.now());
        attendance.setClockIn(time);
        attendance.setTotalHours("0:00:00");
        attendance.setOtHours("0:00:00");

        return attendanceRepository.save(attendance);
    }

    // Clock-out logic with improved validation
    public Attendance clockOut(Long employeeId, LocalTime time) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        List<Attendance> todayAttendances = attendanceRepository.findByEmployeeAndDate(employee, LocalDate.now());
        if (todayAttendances.isEmpty()) {
            throw new IllegalStateException("No clock-in record found for today");
        }

        Attendance attendance = todayAttendances.stream()
                .filter(a -> a.getClockOut() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Employee already clocked out"));

        LocalTime clockIn = attendance.getClockIn();
        if (clockIn != null) {
            boolean invalidTime = false;
            if (time.isBefore(clockIn)) {
                Duration duration = Duration.between(time, clockIn);
                if (duration.toHours() >= 16) {
                    invalidTime = true;
                }
            }
            if (invalidTime) {
                throw new IllegalArgumentException("Clock out time cannot be before clock in time");
            }
        }

        attendance.setClockOut(time);
        attendance.setTotalHours(calculateTotalHours(clockIn, time));
        attendance.setOtHours(calculateOTHours(clockIn, time, LocalTime.of(16, 0)));

        return attendanceRepository.save(attendance);
    }

    // Calculate total hours between clock-in and clock-out
    public String calculateTotalHours(LocalTime clockIn, LocalTime clockOut) {
        if (clockIn == null || clockOut == null) {
            return "0:00:00";
        }

        Duration duration;
        if (clockOut.isBefore(clockIn)) {
            // Overnight shift (e.g. clockIn 22:00, clockOut 06:00)
            Duration firstPart = Duration.between(clockIn, LocalTime.MAX);
            Duration secondPart = Duration.between(LocalTime.MIN, clockOut);
            duration = firstPart.plus(secondPart).plusSeconds(1); // Add one second for midnight edge case
        } else {
            duration = Duration.between(clockIn, clockOut);
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    // Calculate overtime hours (after standard end time)
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
