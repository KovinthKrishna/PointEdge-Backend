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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@RestController
@RequestMapping("/api/attendances")
@CrossOrigin
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
    public ResponseEntity<?> searchAttendances(@RequestBody AttendanceSearchDTO searchDTO) {
        try {
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
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
            .body(Collections.emptyList());
        }
    }

    @GetMapping("/employee/{employeeId}/date-range")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByEmployeeAndDateRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            List<Attendance> attendances = attendanceService.findByEmployeeAndDateBetween(employee, startDate, endDate);
            List<AttendanceDTO> attendanceDTOs = attendances.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(attendanceDTOs);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
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
            }

            // Validate times if both are present
            if (attendance.getClockIn() != null && attendance.getClockOut() != null) {
                validateAttendanceTimes(attendance.getClockIn(), attendance.getClockOut());
                
                // Calculate hours
                attendance.setTotalHours(attendanceService.calculateTotalHours(
                        attendance.getClockIn(), attendance.getClockOut()));
                attendance.setOtHours(attendanceService.calculateOTHours(
                        attendance.getClockIn(), attendance.getClockOut(), LocalTime.of(16, 0)));
            } else {
                // If one time is missing, set to zero hours
                attendance.setTotalHours("0:00:00");
                attendance.setOtHours("0:00:00");
            }

            Attendance savedAttendance = attendanceService.saveAttendance(attendance);
            return ResponseEntity.ok(convertToDTO(savedAttendance));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


    
    
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceDTO> updateAttendance(
            @PathVariable Long id, 
            @RequestBody AttendanceDTO dto) {
        try {
            Attendance attendance = attendanceService.getAttendanceById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
            
            // Only update allowed fields
            if (dto.getEmployeeId() != null) {
                Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());
                attendance.setEmployee(employee);
            }
            
            if (dto.getDate() != null) {
                attendance.setDate(dto.getDate());
            }
            
            // Update clock times
            LocalTime clockIn = attendance.getClockIn();
            LocalTime clockOut = attendance.getClockOut();
            
            if (dto.getClockIn() != null && !dto.getClockIn().isEmpty()) {
                clockIn = LocalTime.parse(dto.getClockIn());
                attendance.setClockIn(clockIn);
            }
            
            if (dto.getClockOut() != null && !dto.getClockOut().isEmpty()) {
                clockOut = LocalTime.parse(dto.getClockOut());
                attendance.setClockOut(clockOut);
            }
            
            // Validate and recalculate hours if both times present
            if (clockIn != null && clockOut != null) {
                validateAttendanceTimes(clockIn, clockOut);
                
                attendance.setTotalHours(attendanceService.calculateTotalHours(clockIn, clockOut));
                attendance.setOtHours(attendanceService.calculateOTHours(
                        clockIn, clockOut, LocalTime.of(16, 0)));
            }
            
            Attendance updatedAttendance = attendanceService.saveAttendance(attendance);
            return ResponseEntity.ok(convertToDTO(updatedAttendance));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Validates that the attendance times are logical
     * @param clockIn The clock-in time
     * @param clockOut The clock-out time
     * @throws IllegalArgumentException if times are invalid
     */
    private void validateAttendanceTimes(LocalTime clockIn, LocalTime clockOut) {
        if (clockIn != null && clockOut != null) {
            // Calculate duration between times
            Duration duration;
            if (clockOut.isBefore(clockIn)) {
                // Overnight shift
                Duration firstPart = Duration.between(clockIn, LocalTime.MAX);
                Duration secondPart = Duration.between(LocalTime.MIN, clockOut);
                duration = firstPart.plus(secondPart).plusSeconds(1);
            } else {
                duration = Duration.between(clockIn, clockOut);
            }
            
            // Validate duration (for example, maximum 16 hours shift)
            if (duration.toHours() > 16) {
                throw new IllegalArgumentException("Shift duration cannot exceed 16 hours");
            }
            
            // Ensure duration is not negative or zero
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("Clock-out time must be after clock-in time");
            }
        }
    }

    private AttendanceDTO convertToDTO(Attendance attendance) {
        AttendanceDTO dto = new AttendanceDTO();

        Employee employee = attendance.getEmployee();
        if (employee != null) {
            dto.setEmployeeId(employee.getId());
            dto.setEmployeeName(employee.getName() != null ? employee.getName() : "");
            dto.setRole(employee.getRole() != null ? employee.getRole() : "");
            dto.setStatus(employee.getStatus() != null ? employee.getStatus().toString() : "Unknown");
            dto.setAvatar(employee.getAvatar() != null ? employee.getAvatar() : "");
        } else {
            // Set default values for null employee
            dto.setEmployeeId(null);
            dto.setEmployeeName("Unknown");
            dto.setRole("Unknown");
            dto.setStatus("Unknown");
            dto.setAvatar("");
        }
        
        dto.setClockIn(attendance.getClockIn() != null ? attendance.getClockIn().toString() : "");
        dto.setClockOut(attendance.getClockOut() != null ? attendance.getClockOut().toString() : "");
        dto.setTotalHours(attendance.getTotalHours() != null ? attendance.getTotalHours() : "0:00:00");
        dto.setOtHours(attendance.getOtHours() != null ? attendance.getOtHours() : "0:00:00");
        dto.setDate(attendance.getDate());
        
        return dto;
    }
}