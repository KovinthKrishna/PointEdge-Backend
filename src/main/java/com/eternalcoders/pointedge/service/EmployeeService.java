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

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Company work time constants
    private static final LocalTime COMPANY_START_TIME = LocalTime.of(8, 0);  // 8:00 AM
    private static final LocalTime COMPANY_END_TIME = LocalTime.of(17, 0);   // 5:00 PM
    private static final int STANDARD_WORK_HOURS = 9;

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

    public Optional<Employee> findByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    // Work time management methods
    public LocalTime getCompanyStartTime() {
        return COMPANY_START_TIME;
    }

    public LocalTime getCompanyEndTime() {
        return COMPANY_END_TIME;
    }

    public int getStandardWorkHours() {
        return STANDARD_WORK_HOURS;
    }

    public Map<String, Object> getWorkSchedule() {
        Map<String, Object> workSchedule = new HashMap<>();
        workSchedule.put("startTime", COMPANY_START_TIME.toString());
        workSchedule.put("endTime", COMPANY_END_TIME.toString());
        workSchedule.put("standardHours", STANDARD_WORK_HOURS);
        workSchedule.put("timezone", "UTC");
        return workSchedule;
    }

    public boolean isWithinWorkHours(LocalTime time) {
        return (time.isAfter(COMPANY_START_TIME) || time.equals(COMPANY_START_TIME)) && 
               (time.isBefore(COMPANY_END_TIME) || time.equals(COMPANY_END_TIME));
    }

    public boolean isLateArrival(LocalTime clockIn) {
        return clockIn.isAfter(COMPANY_START_TIME);
    }

    public boolean isEarlyDeparture(LocalTime clockOut) {
        return clockOut.isBefore(COMPANY_END_TIME);
    }

    public String calculateLateTime(LocalTime clockIn) {
        if (!isLateArrival(clockIn)) {
            return "0:00:00";
        }
        
        Duration lateDuration = Duration.between(COMPANY_START_TIME, clockIn);
        long hours = lateDuration.toHours();
        long minutes = lateDuration.toMinutesPart();
        long seconds = lateDuration.toSecondsPart();
        
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public String calculateEarlyTime(LocalTime clockOut) {
        if (!isEarlyDeparture(clockOut)) {
            return "0:00:00";
        }
        
        Duration earlyDuration = Duration.between(clockOut, COMPANY_END_TIME);
        long hours = earlyDuration.toHours();
        long minutes = earlyDuration.toMinutesPart();
        long seconds = earlyDuration.toSecondsPart();
        
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public String getWorkingHoursRange() {
        return COMPANY_START_TIME.toString() + " - " + COMPANY_END_TIME.toString();
    }

    public Duration getStandardWorkDuration() {
        return Duration.between(COMPANY_START_TIME, COMPANY_END_TIME);
    }

    // Helper method to validate if given times are valid work hours
    public boolean isValidWorkSchedule(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        
        Duration workDuration;
        if (endTime.isBefore(startTime)) {
            // Overnight shift
            Duration firstPart = Duration.between(startTime, LocalTime.MAX);
            Duration secondPart = Duration.between(LocalTime.MIN, endTime);
            workDuration = firstPart.plus(secondPart).plusSeconds(1);
        } else {
            workDuration = Duration.between(startTime, endTime);
        }
        
        // Validate work duration is reasonable (between 1 and 16 hours)
        long hours = workDuration.toHours();
        return hours >= 1 && hours <= 16;
    }

    // Method to check if employee should be considered for overtime
    public boolean isEligibleForOvertime(LocalTime clockOut) {
        return clockOut.isAfter(COMPANY_END_TIME);
    }

    // Method to get break time duration (assuming 1 hour lunch break)
    public Duration getLunchBreakDuration() {
        return Duration.ofHours(1);
    }

    // Method to get effective working hours (excluding break)
    public int getEffectiveWorkingHours() {
        return STANDARD_WORK_HOURS - 1; // 9 hours - 1 hour lunch = 8 effective hours
    }
}