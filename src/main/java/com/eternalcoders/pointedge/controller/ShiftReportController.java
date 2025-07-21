package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.ShiftReportDTO;
import com.eternalcoders.pointedge.service.ShiftReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shift-reports")
@CrossOrigin
public class ShiftReportController {
    
    @Autowired
    private ShiftReportService shiftReportService;
    
    /**
     * Get combined shift and performance data for a specific employee (all time)
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ShiftReportDTO>> getEmployeeShiftReport(@PathVariable Long employeeId) {
        try {
            List<ShiftReportDTO> shiftReports = shiftReportService.getEmployeeShiftReport(employeeId);
            System.out.println("📊 Returning shift report for employee " + employeeId + ": " + shiftReports.size() + " records");
            return ResponseEntity.ok(shiftReports);
        } catch (Exception e) {
            System.err.println("❌ Error getting shift report for employee " + employeeId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get combined shift and performance data for a specific employee within date range
     */
    @GetMapping("/employee/{employeeId}/date-range")
    public ResponseEntity<List<ShiftReportDTO>> getEmployeeShiftReportByDateRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<ShiftReportDTO> shiftReports = shiftReportService.getEmployeeShiftReportByDateRange(employeeId, startDate, endDate);
            System.out.println("📅 Returning shift report for employee " + employeeId + " from " + startDate + " to " + endDate + ": " + shiftReports.size() + " records");
            return ResponseEntity.ok(shiftReports);
        } catch (Exception e) {
            System.err.println("❌ Error getting shift report for employee " + employeeId + " with date range: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get combined shift and performance data for all employees
     */
    @GetMapping("/all")
    public ResponseEntity<List<ShiftReportDTO>> getAllEmployeesShiftReport() {
        try {
            List<ShiftReportDTO> shiftReports = shiftReportService.getAllEmployeesShiftReport();
            System.out.println("🌍 Returning shift reports for all employees: " + shiftReports.size() + " total records");
            return ResponseEntity.ok(shiftReports);
        } catch (Exception e) {
            System.err.println("❌ Error getting all employees shift report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get shift reports for all employees within date range
     */
    @GetMapping("/all/date-range")
    public ResponseEntity<List<ShiftReportDTO>> getAllEmployeesShiftReportByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            // This could be implemented to get all employees' data within date range
            // For now, we'll get all employees and filter their data
            List<ShiftReportDTO> allReports = shiftReportService.getAllEmployeesShiftReport();
            
            // Filter reports within date range
            List<ShiftReportDTO> filteredReports = allReports.stream()
                .filter(report -> {
                    LocalDate shiftDate = report.getShiftDate();
                    return shiftDate != null && 
                           !shiftDate.isBefore(startDate) && 
                           !shiftDate.isAfter(endDate);
                })
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("📅 Returning filtered shift reports from " + startDate + " to " + endDate + ": " + filteredReports.size() + " records");
            return ResponseEntity.ok(filteredReports);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting all employees shift report with date range: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}