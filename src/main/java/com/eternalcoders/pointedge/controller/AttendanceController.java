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
import java.time.format.DateTimeFormatter;
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
        // This is a simplified example. In a real implementation, you would build more complex queries
        LocalDate date = LocalDate.parse(searchDTO.getDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        List<Attendance> attendances = attendanceService.findByDate(date);
        
        List<AttendanceDTO> result = attendances.stream()
                .filter(a -> searchDTO.getSearchQuery().isEmpty() || 
                        a.getEmployee().getName().toLowerCase().contains(searchDTO.getSearchQuery().toLowerCase()) ||
                        a.getEmployee().getRole().toLowerCase().contains(searchDTO.getSearchQuery().toLowerCase()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(result);
    }

    @PostMapping("/clock-in/{employeeId}")
    public ResponseEntity<AttendanceDTO> clockIn(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String time) {
        
        LocalTime clockInTime;
        if (time != null && !time.isEmpty()) {
            clockInTime = LocalTime.parse(time);
        } else {
            clockInTime = LocalTime.now();
        }
        
        Attendance attendance = attendanceService.clockIn(employeeId, clockInTime);
        return ResponseEntity.ok(convertToDTO(attendance));
    }

    @PostMapping("/clock-out/{employeeId}")
    public ResponseEntity<AttendanceDTO> clockOut(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String time) {
        
        LocalTime clockOutTime;
        if (time != null && !time.isEmpty()) {
            clockOutTime = LocalTime.parse(time);
        } else {
            clockOutTime = LocalTime.now();
        }
        
        Attendance attendance = attendanceService.clockOut(employeeId, clockOutTime);
        return ResponseEntity.ok(convertToDTO(attendance));
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