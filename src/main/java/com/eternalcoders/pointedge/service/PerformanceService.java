package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.PerformanceDTO;

import java.time.LocalDate;
import java.util.List;

public interface PerformanceService {
    List<PerformanceDTO> getTopPerformers(LocalDate startDate, LocalDate endDate, String sortBy, String sortDirection);
    List<PerformanceDTO> searchEmployeePerformance(String query, LocalDate startDate, LocalDate endDate);
}