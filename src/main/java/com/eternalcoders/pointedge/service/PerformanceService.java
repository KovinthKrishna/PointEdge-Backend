package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.PerformanceDTO;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.entity.SalesTransaction;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import com.eternalcoders.pointedge.repository.SalesTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class PerformanceService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    @Autowired
    public PerformanceService(
            SalesTransactionRepository salesTransactionRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Get all employee performance data without date restrictions (All Time)
     */
    public List<PerformanceDTO> getAllEmployeePerformance(String sortBy, String sortDirection) {
        List<Employee> employees = employeeRepository.findAll();
        List<PerformanceDTO> performanceDTOs = new ArrayList<>();

        for (Employee employee : employees) {
            performanceDTOs.add(calculateAllTimeEmployeePerformance(employee));
        }

        return sortPerformanceData(performanceDTOs, sortBy, sortDirection);
    }

    /**
     * Get employee performance for specific date range
     */
    public List<PerformanceDTO> getTopPerformers(LocalDate startDate, LocalDate endDate, String sortBy, String sortDirection) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Employee> employees = employeeRepository.findAll();
        List<PerformanceDTO> performanceDTOs = new ArrayList<>();

        for (Employee employee : employees) {
            performanceDTOs.add(calculateEmployeePerformance(employee, startDate, endDate));
        }

        return sortPerformanceData(performanceDTOs, sortBy, sortDirection);
    }

    /**
     * Search employee performance with optional filters
     */
    public List<PerformanceDTO> searchEmployeePerformance(String query, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Employee> employees;
        if (query != null && !query.isEmpty()) {
            employees = employeeRepository.findByNameContainingIgnoreCase(query);

            try {
                Long employeeId = Long.parseLong(query);
                employeeRepository.findById(employeeId).ifPresent(employee -> {
                    if (!employees.contains(employee)) {
                        employees.add(employee);
                    }
                });
            } catch (NumberFormatException ignored) {
            }
        } else {
            employees = employeeRepository.findAll();
        }

        List<PerformanceDTO> performanceDTOs = new ArrayList<>();
        for (Employee employee : employees) {
            performanceDTOs.add(calculateEmployeePerformance(employee, startDate, endDate));
        }

        performanceDTOs.sort((a, b) -> b.getSales().compareTo(a.getSales()));
        return performanceDTOs;
    }

    /**
     * Calculate all-time performance for an employee
     */
    private PerformanceDTO calculateAllTimeEmployeePerformance(Employee employee) {
        // Get all sales transactions for this employee
        List<SalesTransaction> allTransactions = salesTransactionRepository.findByEmployee(employee);
        
        Integer totalOrders = allTransactions != null ? allTransactions.size() : 0;
        
        BigDecimal totalSales = allTransactions.stream()
                .map(SalesTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get all attendance records for this employee
        List<Attendance> allAttendances = attendanceRepository.findByEmployee(employee);
        
        // Calculate total working hours from attendance records
        String totalWorkingHours = calculateTotalWorkingHours(allAttendances);

        PerformanceDTO dto = new PerformanceDTO();
        dto.setId(employee.getId().toString());
        dto.setName(employee.getName());
        dto.setAvatar(employee.getAvatar());
        dto.setRole(employee.getRole());
        dto.setOrders(totalOrders);
        dto.setSales(totalSales);
        dto.setWorkingHours(totalWorkingHours);

        return dto;
    }

    /**
     * Calculate performance for an employee within a date range
     */
    private PerformanceDTO calculateEmployeePerformance(Employee employee, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get sales data
        List<SalesTransaction> transactions = salesTransactionRepository
                .findByEmployeeAndTransactionDateTimeBetween(employee, startDateTime, endDateTime);

        Integer orderCount = transactions != null ? transactions.size() : 0;

        BigDecimal totalSales = transactions.stream()
                .map(SalesTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get attendance data for the date range
        List<Attendance> attendances = attendanceRepository
                .findByEmployeeAndDateBetween(employee, startDate, endDate);

        // Calculate working hours from attendance records
        String totalWorkingHours = calculateTotalWorkingHours(attendances);

        PerformanceDTO dto = new PerformanceDTO();
        dto.setId(employee.getId().toString());
        dto.setName(employee.getName());
        dto.setAvatar(employee.getAvatar());
        dto.setRole(employee.getRole());
        dto.setOrders(orderCount);
        dto.setSales(totalSales);
        dto.setWorkingHours(totalWorkingHours);

        return dto;
    }

    /**
     * Calculate total working hours from attendance records
     * Uses the totalHours field directly from the attendance table
     */
    private String calculateTotalWorkingHours(List<Attendance> attendances) {
        Duration totalDuration = Duration.ZERO;
        
        for (Attendance attendance : attendances) {
            if (attendance.getTotalHours() != null && !attendance.getTotalHours().isEmpty()) {
                Duration attendanceDuration = parseDuration(attendance.getTotalHours());
                totalDuration = totalDuration.plus(attendanceDuration);
            }
        }

        // Format as HH:MM:SS
        long hours = totalDuration.toHours();
        long minutes = totalDuration.toMinutesPart();
        long seconds = totalDuration.toSecondsPart();
        
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Parse duration from time string (HH:MM:SS or HH:MM format)
     */
    private Duration parseDuration(String timeString) {
        if (timeString == null || timeString.isEmpty() || timeString.equals("0:00:00") || timeString.equals("0:00")) {
            return Duration.ZERO;
        }

        String[] parts = timeString.split(":");
        try {
            if (parts.length == 2) {
                // HH:MM format
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                return Duration.ofHours(hours).plusMinutes(minutes);
            } else if (parts.length == 3) {
                // HH:MM:SS format
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2]);
                return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing time string: " + timeString);
        }
        
        return Duration.ZERO;
    }

    /**
     * Sort performance data based on criteria
     */
    private List<PerformanceDTO> sortPerformanceData(List<PerformanceDTO> performanceDTOs, String sortBy, String sortDirection) {
        if (sortBy != null && !sortBy.isEmpty()) {
            boolean isAscending = "asc".equalsIgnoreCase(sortDirection);

            switch (sortBy.toLowerCase()) {
                case "orders":
                    performanceDTOs.sort((a, b) -> isAscending ?
                            a.getOrders().compareTo(b.getOrders()) :
                            b.getOrders().compareTo(a.getOrders()));
                    break;
                case "sales":
                    performanceDTOs.sort((a, b) -> isAscending ?
                            a.getSales().compareTo(b.getSales()) :
                            b.getSales().compareTo(a.getSales()));
                    break;
                case "workinghours":
                    performanceDTOs.sort((a, b) -> {
                        Duration durationA = parseDuration(a.getWorkingHours());
                        Duration durationB = parseDuration(b.getWorkingHours());
                        return isAscending ? durationA.compareTo(durationB) : durationB.compareTo(durationA);
                    });
                    break;
                case "name":
                    performanceDTOs.sort((a, b) -> isAscending ?
                            a.getName().compareTo(b.getName()) :
                            b.getName().compareTo(a.getName()));
                    break;
                default:
                    // Default sort by sales descending
                    performanceDTOs.sort((a, b) -> b.getSales().compareTo(a.getSales()));
            }
        } else {
            // Default sort by sales descending
            performanceDTOs.sort((a, b) -> b.getSales().compareTo(a.getSales()));
        }

        return performanceDTOs;
    }
}