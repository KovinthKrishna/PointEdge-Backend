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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PerformanceServiceImpl implements PerformanceService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;

    @Autowired
    public PerformanceServiceImpl(
            SalesTransactionRepository salesTransactionRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            AttendanceService attendanceService) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
    }

    @Override
    public List<PerformanceDTO> getTopPerformers(LocalDate startDate, LocalDate endDate, String sortBy, String sortDirection) {
        // If dates are not provided, default to current month
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get all employees
        List<Employee> employees = employeeRepository.findAll();
        List<PerformanceDTO> performanceDTOs = new ArrayList<>();

        // For each employee, calculate performance metrics
        for (Employee employee : employees) {
            PerformanceDTO dto = calculateEmployeePerformance(employee, startDate, endDate);
            performanceDTOs.add(dto);
        }

        // Sort the results
        if (sortBy != null && !sortBy.isEmpty()) {
            boolean isAscending = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
            
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
                        return isAscending ? 
                                durationA.compareTo(durationB) : 
                                durationB.compareTo(durationA);
                    });
                    break;
                default:
                    // Default sort by sales
                    performanceDTOs.sort((a, b) -> b.getSales().compareTo(a.getSales()));
            }
        } else {
            // Default sort by sales
            performanceDTOs.sort((a, b) -> b.getSales().compareTo(a.getSales()));
        }

        return performanceDTOs;
    }

    @Override
    public List<PerformanceDTO> searchEmployeePerformance(String query, LocalDate startDate, LocalDate endDate) {
        // If dates are not provided, default to current month
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Employee> employees;
        if (query != null && !query.isEmpty()) {
            employees = employeeRepository.findByNameContainingIgnoreCase(query);
            
            // If query might be an employee ID
            try {
                Long employeeId = Long.parseLong(query);
                employeeRepository.findById(employeeId).ifPresent(employee -> {
                    if (!employees.contains(employee)) {
                        employees.add(employee);
                    }
                });
            } catch (NumberFormatException ignored) {
                // Not a number, just continue
            }
        } else {
            employees = employeeRepository.findAll();
        }

        List<PerformanceDTO> performanceDTOs = new ArrayList<>();
        for (Employee employee : employees) {
            PerformanceDTO dto = calculateEmployeePerformance(employee, startDate, endDate);
            performanceDTOs.add(dto);
        }

        // Default sort by sales descending
        performanceDTOs.sort((a, b) -> b.getSales().compareTo(a.getSales()));
        
        return performanceDTOs;
    }

    private PerformanceDTO calculateEmployeePerformance(Employee employee, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // Calculate sales metrics
        List<SalesTransaction> transactions = salesTransactionRepository
                .findByEmployeeAndTransactionDateTimeBetween(employee, startDateTime, endDateTime);
        
        // Count distinct orders
        Integer orderCount = salesTransactionRepository
                .countOrdersByEmployeeAndDateRange(employee, startDateTime, endDateTime);
        
        // Calculate total sales
        BigDecimal totalSales = transactions.stream()
                .map(SalesTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate working hours
        List<Attendance> attendances = attendanceRepository
                .findByEmployeeAndDateBetween(employee, startDate, endDate);
        
        Duration totalWorkingHours = Duration.ZERO;
        for (Attendance attendance : attendances) {
            if (attendance.getClockIn() != null && attendance.getClockOut() != null) {
                String hoursString = attendance.getTotalHours();
                Duration duration = parseDuration(hoursString);
                totalWorkingHours = totalWorkingHours.plus(duration);
            }
        }
        
        // Format the hours
        String formattedHours = String.format("%d:%02d:%02d", 
                totalWorkingHours.toHours(), 
                totalWorkingHours.toMinutesPart(), 
                totalWorkingHours.toSecondsPart());

        PerformanceDTO dto = new PerformanceDTO();
        dto.setId(employee.getId().toString());
        dto.setName(employee.getName());
        dto.setAvatar(employee.getAvatar());
        dto.setRole(employee.getRole());
        dto.setOrders(orderCount != null ? orderCount : 0);
        dto.setSales(totalSales);
        dto.setWorkingHours(formattedHours);
        
        return dto;
    }
    
    private Duration parseDuration(String timeString) {
        if (timeString == null || timeString.isEmpty() || timeString.equals("0:00:00")) {
            return Duration.ZERO;
        }

        String[] parts = timeString.split(":");
        if (parts.length != 3) {
            return Duration.ZERO;
        }

        try {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            
            return Duration.ofHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds);
        } catch (NumberFormatException e) {
            return Duration.ZERO;
        }
    }
}