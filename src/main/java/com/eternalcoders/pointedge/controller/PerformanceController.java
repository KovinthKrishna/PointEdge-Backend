package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.PerformanceDTO;
import com.eternalcoders.pointedge.service.PerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        List<PerformanceDTO> topPerformers = performanceService.getTopPerformers(startDate, endDate, sortBy, sortDirection);
        return ResponseEntity.ok(topPerformers);
    }
    

    @GetMapping("/search")
    public ResponseEntity<List<PerformanceDTO>> searchEmployeePerformance(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<PerformanceDTO> performance = performanceService.searchEmployeePerformance(query, startDate, endDate);
        return ResponseEntity.ok(performance);
    }
}