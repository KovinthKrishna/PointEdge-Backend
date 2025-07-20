package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.PerformanceDTO;
import com.eternalcoders.pointedge.dto.ShiftReportDTO;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.repository.AttendanceRepository;
import com.eternalcoders.pointedge.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftReportService {
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private PerformanceService performanceService;
    
    public List<ShiftReportDTO> getEmployeeShiftReport(Long employeeId) {
        try {
            // Get employee details
            Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));
            
            // ✅ Fix: Use existing method and sort manually
            List<Attendance> attendances = attendanceRepository.findByEmployee(employee)
                .stream()
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate())) // Sort by date descending
                .collect(Collectors.toList());
            
            // Get performance data for this employee (all time)
            PerformanceDTO performanceData = performanceService.getEmployeeAllTimeTotals(employeeId);
            
            // Combine the data
            return attendances.stream().map(attendance -> {
                String shiftType = determineShiftType(attendance.getClockIn());
                
                return new ShiftReportDTO(
                    employee.getId(),
                    employee.getName(),
                    employee.getRole(),
                    attendance.getDate(),
                    attendance.getClockIn(),
                    attendance.getClockOut(),
                    attendance.getOtHours(),
                    attendance.getTotalHours(),
                    shiftType,
                    performanceData != null ? performanceData.getTotalOrders() : 0,
                    performanceData != null ? performanceData.getTotalSales() : 0.0,
                    performanceData != null ? performanceData.getWorkingHours() : "0:00:00"
                );
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("Error getting shift report for employee " + employeeId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate shift report for employee: " + employeeId, e);
        }
    }
    
    public List<ShiftReportDTO> getAllEmployeesShiftReport() {
        try {
            List<Employee> employees = employeeRepository.findAll();
            
            return employees.stream()
                .flatMap(employee -> {
                    try {
                        return getEmployeeShiftReport(employee.getId()).stream();
                    } catch (Exception e) {
                        System.err.println("Error processing employee " + employee.getId() + ": " + e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Error getting all employees shift report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate shift reports for all employees", e);
        }
    }
    
    public List<ShiftReportDTO> getEmployeeShiftReportByDateRange(Long employeeId, 
                                                                 java.time.LocalDate startDate, 
                                                                 java.time.LocalDate endDate) {
        try {
            Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));
            
            // Get attendance records for this employee within date range
            List<Attendance> attendances = attendanceRepository.findByEmployeeAndDateBetween(employee, startDate, endDate);
            
            // Get performance data for this employee within date range
            PerformanceDTO performanceData = performanceService.getEmployeeTotalsByPeriod(employeeId, startDate, endDate);
            
            return attendances.stream().map(attendance -> {
                String shiftType = determineShiftType(attendance.getClockIn());
                
                return new ShiftReportDTO(
                    employee.getId(),
                    employee.getName(),
                    employee.getRole(),
                    attendance.getDate(),
                    attendance.getClockIn(),
                    attendance.getClockOut(),
                    attendance.getOtHours(),
                    attendance.getTotalHours(),
                    shiftType,
                    performanceData != null ? performanceData.getTotalOrders() : 0,
                    performanceData != null ? performanceData.getTotalSales() : 0.0,
                    performanceData != null ? performanceData.getWorkingHours() : "0:00:00"
                );
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("Error getting shift report for employee " + employeeId + " with date range: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate shift report for employee: " + employeeId, e);
        }
    }
    
    private String determineShiftType(LocalTime clockIn) {
        if (clockIn == null) return "Regular Shift";
        
        int hour = clockIn.getHour();
        if (hour >= 5 && hour < 12) return "Morning Shift";
        else if (hour >= 12 && hour < 17) return "Afternoon Shift";
        else if (hour >= 17 && hour < 22) return "Evening Shift";
        else return "Night Shift";
    }
}