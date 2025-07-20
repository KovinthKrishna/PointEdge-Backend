package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.PerformanceDTO;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import com.eternalcoders.pointedge.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class PerformanceService {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    @Autowired
    public PerformanceService(
            OrderRepository orderRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository) {
        this.orderRepository = orderRepository;
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

    // ✅ Updated to use Order table instead of SalesTransaction
    private PerformanceDTO calculateAllTimeEmployeePerformance(Employee employee) {
        // Get all orders for this employee using employeeId
        List<Order> allOrders = orderRepository.findByEmployeeId(employee.getId());
        List<Attendance> allAttendances = attendanceRepository.findByEmployee(employee);

        return buildPerformanceDTO(employee, allOrders, allAttendances);
    }

    // ✅ Updated to use Order table instead of SalesTransaction
    private PerformanceDTO calculateEmployeePerformance(Employee employee, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get orders for this employee within date range
        List<Order> orders = orderRepository.findByEmployeeIdAndOrderDateBetween(
                employee.getId(), startDateTime, endDateTime);

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeAndDateBetween(employee, startDate, endDate);

        return buildPerformanceDTO(employee, orders, attendances);
    }

    // ✅ Updated to use Order list instead of SalesTransaction list
    private PerformanceDTO buildPerformanceDTO(Employee employee, List<Order> orders, List<Attendance> attendances) {
        // Count total number of orders
        Integer orderCount = orders != null ? orders.size() : 0;

        // Calculate total sales amount from order.total field
        Double totalSalesAmount = orders.stream()
                .filter(order -> order.getTotal() != null)  // Filter out null totals
                .mapToDouble(Order::getTotal)               // Get total from each order
                .sum();                                     // Sum all totals

        String totalWorkingHours = calculateTotalWorkingHours(attendances);

        PerformanceDTO dto = new PerformanceDTO();
        dto.setId(employee.getId().toString());
        dto.setName(employee.getName());
        dto.setAvatar(employee.getAvatar());
        dto.setRole(employee.getRole());
        dto.setTotalOrders(orderCount);
        dto.setTotalSales(totalSalesAmount);  // Now using Double directly
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
                comparator = Comparator.comparing(PerformanceDTO::getTotalOrders);
                break;
            case "sales":
                comparator = Comparator.comparing(PerformanceDTO::getTotalSales);
                break;
            case "workinghours":
                comparator = Comparator.comparing(dto -> parseDuration(dto.getWorkingHours()));
                break;
            case "name":
                comparator = Comparator.comparing(PerformanceDTO::getName);
                break;
            default:
                comparator = Comparator.comparing(PerformanceDTO::getTotalSales);
        }

        if (!isAscending) {
            comparator = comparator.reversed();
        }

        performanceDTOs.sort(comparator);
        return performanceDTOs;
    }
}