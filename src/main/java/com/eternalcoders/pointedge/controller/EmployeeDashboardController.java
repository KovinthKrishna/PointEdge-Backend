package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.EmployeeDashboardDTO;
import com.eternalcoders.pointedge.dto.EmployeeDashboardDTO.MonthlyProductivity;
import com.eternalcoders.pointedge.dto.EmployeeDashboardDTO.DailyAttendance;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.OrderRepository;
import com.eternalcoders.pointedge.service.AttendanceService;
import com.eternalcoders.pointedge.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class EmployeeDashboardController {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final OrderRepository orderRepository; 

    // Constants for productivity calculation
    private static final int STANDARD_MONTHLY_WORKING_HOURS = 160; // 160 hours per month
    private static final int MAX_OT_HOURS_PER_EMPLOYEE = 20; // Maximum 20 OT hours per employee per month

    @Autowired
    public EmployeeDashboardController(EmployeeService employeeService, AttendanceService attendanceService, OrderRepository orderRepository) {
        this.employeeService = employeeService;
        this.attendanceService = attendanceService;
        this.orderRepository = orderRepository; // ✅ Add this
    }

   
     //Get all dashboard data in a single API call

    @GetMapping("/employee-stats")
    public ResponseEntity<EmployeeDashboardDTO> getEmployeeDashboard() {
        EmployeeDashboardDTO dashboard = new EmployeeDashboardDTO();

        // ✅ Get total orders and sales from orders table
        long totalOrders = orderRepository.count();
        double totalSales = orderRepository.findAll()
            .stream()
            .mapToDouble(order -> order.getTotal() != null ? order.getTotal() : 0.0)
            .sum();
        dashboard.setTotalOrders(totalOrders);
        dashboard.setTotalSales(totalSales);

        // Get total employee count
        List<Employee> employees = employeeService.getAllEmployees();
        dashboard.setTotalEmployees(employees.size());

        // Get active vs leave employees - Updated to use correct enum values
        long activeEmployees = employees.stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.Active)
                .count();
        long onLeaveEmployees = employees.stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.Leave)
                .count();

        dashboard.setActiveEmployees((int) activeEmployees);
        dashboard.setOnLeaveEmployees((int) onLeaveEmployees);

        // Calculate total hours worked this month
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        List<Attendance> thisMonthAttendances = attendanceService.findByDateBetween(firstDayOfMonth, today);

        long totalMinutesWorked = 0;
        for (Attendance attendance : thisMonthAttendances) {
            if (attendance.getTotalHours() != null && !attendance.getTotalHours().isEmpty()) {
                String[] parts = attendance.getTotalHours().split(":");
                if (parts.length >= 2) {
                    totalMinutesWorked += Integer.parseInt(parts[0]) * 60;
                    totalMinutesWorked += Integer.parseInt(parts[1]);
                }
            }
        }

        int hoursWorked = (int) (totalMinutesWorked / 60);
        int minutesWorked = (int) (totalMinutesWorked % 60);

        dashboard.setTotalHoursWorked(String.format("%d.%02d h", hoursWorked, minutesWorked));

        // Calculate change percentages compared to previous month
        LocalDate firstDayOfPreviousMonth = firstDayOfMonth.minusMonths(1);
        LocalDate lastDayOfPreviousMonth = firstDayOfMonth.minusDays(1);

        List<Attendance> previousMonthAttendances =
                attendanceService.findByDateBetween(firstDayOfPreviousMonth, lastDayOfPreviousMonth);

        // Calculate employee change percentage
        int previousMonthEmployeeCount = (int) previousMonthAttendances.stream()
                .map(a -> a.getEmployee().getId())
                .distinct()
                .count();

        int employeeChangePercent = previousMonthEmployeeCount > 0 ?
                (int) (((double) employees.size() - previousMonthEmployeeCount) / previousMonthEmployeeCount * 100) : 0;

        dashboard.setEmployeeChangePercent(employeeChangePercent);

        // Calculate hours worked change percentage
        long prevMonthMinutesWorked = 0;
        for (Attendance attendance : previousMonthAttendances) {
            if (attendance.getTotalHours() != null && !attendance.getTotalHours().isEmpty()) {
                String[] parts = attendance.getTotalHours().split(":");
                if (parts.length >= 2) {
                    prevMonthMinutesWorked += Integer.parseInt(parts[0]) * 60;
                    prevMonthMinutesWorked += Integer.parseInt(parts[1]);
                }
            }
        }

        int hoursChangePercent = prevMonthMinutesWorked > 0 ?
                (int) (((double) totalMinutesWorked - prevMonthMinutesWorked) / prevMonthMinutesWorked * 100) : 0;

        dashboard.setHoursChangePercent(hoursChangePercent);

        // Set attendance report data (reuse the same active/leave counts)
        dashboard.setActiveCount((int) activeEmployees);
        dashboard.setLeaveCount((int) onLeaveEmployees);
        dashboard.setActivePercentage(employees.size() > 0 ?
                (int) (activeEmployees * 100.0 / employees.size()) : 0);
        dashboard.setLeavePercentage(employees.size() > 0 ?
                (int) (onLeaveEmployees * 100.0 / employees.size()) : 0);

        // Calculate productivity data
        dashboard.setProductivityData(calculateMonthlyProductivity());

        // Calculate weekly attendance
        dashboard.setWeeklyAttendance(calculateWeeklyAttendance());

        return ResponseEntity.ok(dashboard);
    }


    private List<MonthlyProductivity> calculateMonthlyProductivity() {
        List<MonthlyProductivity> productivityData = new ArrayList<>();
        int year = LocalDate.now().getYear();

        // For each month of the year
        for (int month = 1; month <= 12; month++) {
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
            LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

            // If the month is in the future, use estimated data
            if (firstDayOfMonth.isAfter(LocalDate.now())) {
                MonthlyProductivity monthData = new MonthlyProductivity();
                monthData.setMonth(firstDayOfMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                monthData.setPrimary(0); // Productivity percentage
                monthData.setSecondary(0); // OT hours
                productivityData.add(monthData);
                continue;
            }

            // Get all attendances for this month
            List<Attendance> monthAttendances =
                    attendanceService.findByDateBetween(firstDayOfMonth,
                    lastDayOfMonth.isAfter(LocalDate.now()) ? LocalDate.now() : lastDayOfMonth);

            // Get number of employees who worked this month
            Set<Long> employeesWorked = monthAttendances.stream()
                    .map(a -> a.getEmployee().getId())
                    .collect(Collectors.toSet());

            int numberOfEmployees = employeesWorked.size();

            // If no employees worked, set productivity to 0
            if (numberOfEmployees == 0) {
                MonthlyProductivity monthData = new MonthlyProductivity();
                monthData.setMonth(firstDayOfMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                monthData.setPrimary(0);
                monthData.setSecondary(0);
                productivityData.add(monthData);
                continue;
            }

            // Calculate total hours worked and OT hours
            double totalHoursWorked = 0;
            Map<Long, Double> employeeOTHours = new HashMap<>();

            for (Attendance attendance : monthAttendances) {
                // Calculate total hours
                if (attendance.getTotalHours() != null && !attendance.getTotalHours().isEmpty()) {
                    String[] parts = attendance.getTotalHours().split(":");
                    if (parts.length >= 2) {
                        totalHoursWorked += Integer.parseInt(parts[0]);
                        totalHoursWorked += Integer.parseInt(parts[1]) / 60.0;
                    }
                }

                // Calculate OT hours per employee 
                if (attendance.getOtHours() != null && !attendance.getOtHours().isEmpty()) {
                    String[] parts = attendance.getOtHours().split(":");
                    if (parts.length >= 2) {
                        double otHours = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0;
                        Long employeeId = attendance.getEmployee().getId();

                        employeeOTHours.merge(employeeId, otHours, Double::sum);
                    }
                }
            }

            // Apply 20-hour OT limit per employee and calculate total valid OT hours
            double totalValidOTHours = employeeOTHours.values().stream()
                    .mapToDouble(hours -> Math.min(hours, MAX_OT_HOURS_PER_EMPLOYEE))
                    .sum();

            
            // Productivity = Total Hours Worked / (Number of Employees × Standard Monthly Working Hours)
            double standardTotalHours = numberOfEmployees * STANDARD_MONTHLY_WORKING_HOURS;
            double productivityPercentage = standardTotalHours > 0 ?
                    (totalHoursWorked / standardTotalHours) * 100 : 0;

            // Cap productivity at 100% for display purposes
            productivityPercentage = Math.min(productivityPercentage, 100);

            MonthlyProductivity monthData = new MonthlyProductivity();
            monthData.setMonth(firstDayOfMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            monthData.setPrimary((int) Math.round(productivityPercentage)); // Productivity percentage
            monthData.setSecondary((int) Math.round(totalValidOTHours)); // Total valid OT hours

            productivityData.add(monthData);
        }

        return productivityData;
    }

    // Helper method to calculate weekly attendance
    private List<DailyAttendance> calculateWeeklyAttendance() {
        List<DailyAttendance> weeklyAttendance = new ArrayList<>();

        // Get the date for 7 days ago
        LocalDate startDate = LocalDate.now().minusDays(6);

        // Get all employees to calculate percentage
        int totalEmployees = employeeService.getAllEmployees().size();

        
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);

            // Get attendance for this day
            List<Attendance> dayAttendances = attendanceService.findByDate(date);

            // Count unique employees who attended
            long attendedEmployees = dayAttendances.stream()
                    .map(a -> a.getEmployee().getId())
                    .distinct()
                    .count();

            // Calculate attendance percentage
            int attendancePercentage = totalEmployees > 0 ?
                    (int) (attendedEmployees * 100.0 / totalEmployees) : 0;

            // Calculate a height value based on percentage 
            int height = (int) (attendancePercentage * 1.6); 
            DailyAttendance dayData = new DailyAttendance();
            dayData.setDate(date.toString());
            dayData.setDayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            dayData.setAttendancePercentage(attendancePercentage);
            dayData.setHeight(height);

            weeklyAttendance.add(dayData);
        }

        return weeklyAttendance;
    }

     //Get productivity configuration
     
    @GetMapping("/productivity-config")
    public ResponseEntity<?> getProductivityConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("standardMonthlyWorkingHours", STANDARD_MONTHLY_WORKING_HOURS);
        config.put("maxOTHoursPerEmployee", MAX_OT_HOURS_PER_EMPLOYEE);
        config.put("productivityFormula", "Total Hours Worked / (Number of Employees × Standard Monthly Working Hours)");
        return ResponseEntity.ok(config);

    }

}