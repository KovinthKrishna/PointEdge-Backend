package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.PerformanceDTO;
import com.eternalcoders.pointedge.service.PerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/performance")
@CrossOrigin
public class PerformanceController {

    private final PerformanceService performanceService;

    @Autowired
    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/top-performers")
    public ResponseEntity<List<PerformanceDTO>> getTopPerformers(
        @RequestParam String sortBy,
        @RequestParam String sortDirection,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false, defaultValue = "false") boolean includeAllData
    ) {
        try {
            List<PerformanceDTO> performers;
            
            if (includeAllData) {
                // For "All Time" - get all performance data without date restrictions
                performers = performanceService.getAllEmployeePerformance(sortBy, sortDirection);
                System.out.println("🌍 All Time Request: Fetching all performance data");
            } else if (startDate != null && endDate != null) {
                // For specific date ranges (Last Month, Last Week)
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                performers = performanceService.getTopPerformers(start, end, sortBy, sortDirection);
                System.out.println("📅 Date Range Request: " + startDate + " to " + endDate);
            } else {
                // Default fallback - current month
                LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
                LocalDate today = LocalDate.now();
                performers = performanceService.getTopPerformers(startOfMonth, today, sortBy, sortDirection);
                System.out.println("⚠️ No specific parameters - using current month");
            }
            
            System.out.println("📊 Returning " + performers.size() + " employee records");
            return ResponseEntity.ok(performers);
            
        } catch (Exception e) {
            System.err.println("❌ Error in getTopPerformers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PerformanceDTO>> searchEmployeePerformance(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<PerformanceDTO> performance = performanceService.searchEmployeePerformance(query, startDate, endDate);
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            System.err.println("❌ Error in searchEmployeePerformance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}