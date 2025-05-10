package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.AttendanceDTO;
import com.eternalcoders.pointedge.dto.AttendanceSearchDTO;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.exception.ResourceNotFoundException;
import com.eternalcoders.pointedge.service.AttendanceService;
import com.eternalcoders.pointedge.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendances")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175"})
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;

    @Autowired
    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<List<AttendanceDTO>> getAllAttendances() {
        List<AttendanceDTO> attendances = attendanceService.getAllAttendances().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByEmployee(@PathVariable Long employeeId) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            List<AttendanceDTO> attendances = attendanceService.findByEmployee(employee).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(attendances);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceDTO> attendances = attendanceService.findByDate(date).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attendances);
    }

    @PostMapping("/search")
    public ResponseEntity<List<AttendanceDTO>> searchAttendances(@RequestBody AttendanceSearchDTO searchDTO) {
        try {
            // Get all attendances
            List<Attendance> attendances = attendanceService.getAllAttendances();
            
            // Apply employee ID filter if provided
            if (searchDTO.getEmployeeId() != null) {
                System.out.println("Filtering by employee ID: " + searchDTO.getEmployeeId());
                attendances = attendances.stream()
                    .filter(a -> a.getEmployee().getId().equals(searchDTO.getEmployeeId()))
                    .collect(Collectors.toList());
            }
    
            // Apply search filter on employee name and role
            String searchQuery = searchDTO.getSearchQuery() != null ? 
                    searchDTO.getSearchQuery().toLowerCase() : "";
    
            List<AttendanceDTO> result = attendances.stream()
                    .filter(a -> searchQuery.isEmpty() || 
                            (a.getEmployee().getName() != null && 
                             a.getEmployee().getName().toLowerCase().contains(searchQuery)) ||
                            (a.getEmployee().getRole() != null && 
                             a.getEmployee().getRole().toLowerCase().contains(searchQuery)))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in search: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for better debugging
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<AttendanceDTO> createAttendance(@RequestBody AttendanceDTO dto) {
        try {
            Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());

            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());

            // Parse time strings to LocalTime
            if (dto.getClockIn() != null && !dto.getClockIn().isEmpty()) {
                attendance.setClockIn(LocalTime.parse(dto.getClockIn()));
            }

            if (dto.getClockOut() != null && !dto.getClockOut().isEmpty()) {
                attendance.setClockOut(LocalTime.parse(dto.getClockOut()));

                // Calculate hours if both clock-in and clock-out are present
                if (attendance.getClockIn() != null) {
                    attendance.setTotalHours(attendanceService.calculateTotalHours(
                            attendance.getClockIn(), attendance.getClockOut()));
                    attendance.setOtHours(attendanceService.calculateOTHours(
                            attendance.getClockIn(), attendance.getClockOut(), LocalTime.of(16, 0)));
                }
            }

            Attendance savedAttendance = attendanceService.saveAttendance(attendance);
            return ResponseEntity.ok(convertToDTO(savedAttendance));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private AttendanceDTO convertToDTO(Attendance attendance) {
        AttendanceDTO dto = new AttendanceDTO();

        dto.setEmployeeId(attendance.getEmployee().getId());
        dto.setEmployeeName(attendance.getEmployee().getName());
        dto.setRole(attendance.getEmployee().getRole());
        dto.setClockIn(attendance.getClockIn() != null ? attendance.getClockIn().toString() : "");
        dto.setClockOut(attendance.getClockOut() != null ? attendance.getClockOut().toString() : "");
        dto.setTotalHours(attendance.getTotalHours() != null ? attendance.getTotalHours() : "0:00:00");
        dto.setOtHours(attendance.getOtHours() != null ? attendance.getOtHours() : "0:00:00");
        dto.setStatus(attendance.getEmployee().getStatus().toString());
        dto.setAvatar(attendance.getEmployee().getAvatar());
        dto.setDate(attendance.getDate());
        return dto;
    }
}