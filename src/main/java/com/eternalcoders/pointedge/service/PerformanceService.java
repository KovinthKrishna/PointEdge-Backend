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

    public List<PerformanceDTO> getAllEmployeePerformance(String sortBy, String sortDirection) {
        List<Employee> employees = employeeRepository.findAll();
        List<PerformanceDTO> performanceDTOs = new ArrayList<>();

        for (Employee employee : employees) {
            performanceDTOs.add(calculateAllTimeEmployeePerformance(employee));
        }

        return sortPerformanceData(performanceDTOs, sortBy, sortDirection);
    }

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
            } catch (NumberFormatException ignored) {}
        } else {
            employees = employeeRepository.findAll();
        }

        List<PerformanceDTO> performanceDTOs = new ArrayList<>();
        for (Employee employee : employees) {
            performanceDTOs.add(calculateEmployeePerformance(employee, startDate, endDate));
        }

        return sortPerformanceData(performanceDTOs, "sales", "desc");
    }

    // ✅ Add missing methods for the new controller endpoints
    public List<PerformanceDTO> getAllEmployeeTotals(String sortBy, String sortDirection) {
        return getAllEmployeePerformance(sortBy, sortDirection);
    }

    public List<PerformanceDTO> getEmployeeTotalsByDateRange(LocalDate startDate, LocalDate endDate, String sortBy, String sortDirection) {
        return getTopPerformers(startDate, endDate, sortBy, sortDirection);
    }

    public PerformanceDTO getEmployeeAllTimeTotals(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return null;
        }
        return calculateAllTimeEmployeePerformance(employee);
    }

    public PerformanceDTO getEmployeeTotalsByPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return null;
        }
        return calculateEmployeePerformance(employee, startDate, endDate);
    }

    // ✅ Add other missing methods
    public List<PerformanceDTO> getAllEmployeeSales(String sortDirection) {
        return getAllEmployeePerformance("sales", sortDirection);
    }

    public List<PerformanceDTO> getEmployeeSalesByDateRange(LocalDate startDate, LocalDate endDate, String sortDirection) {
        return getTopPerformers(startDate, endDate, "sales", sortDirection);
    }

    public List<PerformanceDTO> getAllEmployeeOrders(String sortDirection) {
        return getAllEmployeePerformance("orders", sortDirection);
    }

    public List<PerformanceDTO> getEmployeeOrdersByDateRange(LocalDate startDate, LocalDate endDate, String sortDirection) {
        return getTopPerformers(startDate, endDate, "orders", sortDirection);
    }

    public PerformanceDTO getEmployeeAllTimeSales(Long employeeId) {
        return getEmployeeAllTimeTotals(employeeId);
    }

    public PerformanceDTO getEmployeeSalesByPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return getEmployeeTotalsByPeriod(employeeId, startDate, endDate);
    }

    public PerformanceDTO getEmployeeAllTimeOrders(Long employeeId) {
        return getEmployeeAllTimeTotals(employeeId);
    }

    public PerformanceDTO getEmployeeOrdersByPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return getEmployeeTotalsByPeriod(employeeId, startDate, endDate);
    }

    public List<PerformanceDTO> getAllTimeSalesOrdersSummary(String sortBy, String sortDirection) {
        return getAllEmployeePerformance(sortBy, sortDirection);
    }

    public List<PerformanceDTO> getSalesOrdersSummaryByDateRange(LocalDate startDate, LocalDate endDate, String sortBy, String sortDirection) {
        return getTopPerformers(startDate, endDate, sortBy, sortDirection);
    }

    private PerformanceDTO calculateAllTimeEmployeePerformance(Employee employee) {
        List<SalesTransaction> allTransactions = salesTransactionRepository.findByEmployee(employee);
        List<Attendance> allAttendances = attendanceRepository.findByEmployee(employee);

        return buildPerformanceDTO(employee, allTransactions, allAttendances);
    }

    private PerformanceDTO calculateEmployeePerformance(Employee employee, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<SalesTransaction> transactions = salesTransactionRepository
                .findByEmployeeAndTransactionDateTimeBetween(employee, startDateTime, endDateTime);

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeAndDateBetween(employee, startDate, endDate);

        return buildPerformanceDTO(employee, transactions, attendances);
    }

    private PerformanceDTO buildPerformanceDTO(Employee employee, List<SalesTransaction> transactions, List<Attendance> attendances) {
        Integer orderCount = transactions != null ? transactions.size() : 0;

        BigDecimal totalSalesAmount = transactions.stream()
                .map(SalesTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String totalWorkingHours = calculateTotalWorkingHours(attendances);

        PerformanceDTO dto = new PerformanceDTO();
        dto.setId(employee.getId().toString());
        dto.setName(employee.getName());
        dto.setAvatar(employee.getAvatar());
        dto.setRole(employee.getRole());
        // ✅ Fix: Use correct method names that match PerformanceDTO fields
        dto.setTotalOrders(orderCount);  // Changed from setOrders() to setTotalOrders()
        dto.setTotalSales(totalSalesAmount.doubleValue());  // Changed from setSales() to setTotalSales()
        dto.setWorkingHours(totalWorkingHours);

        return dto;
    }

    private String calculateTotalWorkingHours(List<Attendance> attendances) {
        Duration totalDuration = Duration.ZERO;

        for (Attendance attendance : attendances) {
            if (attendance.getTotalHours() != null && !attendance.getTotalHours().isEmpty()) {
                Duration attendanceDuration = parseDuration(attendance.getTotalHours());
                totalDuration = totalDuration.plus(attendanceDuration);
            }
        }

        long hours = totalDuration.toHours();
        long minutes = totalDuration.toMinutesPart();
        long seconds = totalDuration.toSecondsPart();

        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    private Duration parseDuration(String timeString) {
        if (timeString == null || timeString.isEmpty() || timeString.equals("0:00:00") || timeString.equals("0:00")) {
            return Duration.ZERO;
        }

        String[] parts = timeString.split(":");
        try {
            if (parts.length == 2) {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                return Duration.ofHours(hours).plusMinutes(minutes);
            } else if (parts.length == 3) {
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

    private List<PerformanceDTO> sortPerformanceData(List<PerformanceDTO> performanceDTOs, String sortBy, String sortDirection) {
        boolean isAscending = "asc".equalsIgnoreCase(sortDirection);

        Comparator<PerformanceDTO> comparator;

        switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "orders":
                // ✅ Fix: Use correct getter method name
                comparator = Comparator.comparing(PerformanceDTO::getTotalOrders);
                break;
            case "sales":
                // ✅ Fix: Use correct getter method name
                comparator = Comparator.comparing(PerformanceDTO::getTotalSales);
                break;
            case "workinghours":
                comparator = Comparator.comparing(dto -> parseDuration(dto.getWorkingHours()));
                break;
            case "name":
                comparator = Comparator.comparing(PerformanceDTO::getName);
                break;
            default:
                // ✅ Fix: Use correct getter method name
                comparator = Comparator.comparing(PerformanceDTO::getTotalSales);
        }

        if (!isAscending) {
            comparator = comparator.reversed();
        }

        performanceDTOs.sort(comparator);
        return performanceDTOs;
    }
}