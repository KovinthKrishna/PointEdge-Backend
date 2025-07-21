package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.AttendanceDTO;
import com.eternalcoders.pointedge.dto.AttendanceSearchDTO;
import com.eternalcoders.pointedge.entity.Attendance;
import com.eternalcoders.pointedge.entity.Employee;
import com.eternalcoders.pointedge.exception.ResourceNotFoundException;
import com.eternalcoders.pointedge.security.JwtUtil;
import com.eternalcoders.pointedge.service.AttendanceService;
import com.eternalcoders.pointedge.service.EmployeeService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@RestController
@RequestMapping("/api/attendances")
@CrossOrigin
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService, JwtUtil jwtUtil) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.jwtUtil = jwtUtil;
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
            
            // Sort attendances by date and clockIn
            List<Attendance> attendances = attendanceService.findByEmployee(employee)
                .stream()
                .sorted(Comparator.comparing(Attendance::getDate)
                                  .thenComparing(Attendance::getClockIn, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

            List<AttendanceDTO> attendanceDTOs = calculateBreakTimes(attendances);
            return ResponseEntity.ok(attendanceDTOs);
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

            if (searchDTO.getEmployeeId() != null) {
                attendances = attendances.stream()
                    .filter(a -> a.getEmployee().getId().equals(searchDTO.getEmployeeId()))
                    .collect(Collectors.toList());
            }

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

    @PostMapping("/clock-in")
    public ResponseEntity<?> clockIn(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            token = token.substring(7);
            String email = jwtUtil.extractUsername(token);

            Employee employee = employeeService.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

            LocalTime now = LocalTime.now();

            Attendance attendance = attendanceService.clockIn(employee.getId(), now);

            return ResponseEntity.ok("Clock-in recorded at " + now.toString());

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during clock-in");
        }
    }

    @PostMapping("/clock-out")
    public ResponseEntity<?> clockOut(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            token = token.substring(7);
            String email = jwtUtil.extractUsername(token);

            Employee employee = employeeService.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

            LocalTime now = LocalTime.now();

            Attendance attendance = attendanceService.clockOut(employee.getId(), now);

            return ResponseEntity.ok("Clock-out recorded at " + now.toString());

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during clock-out");
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
            if (dto.getEmployeeId() == null) {
                return ResponseEntity.badRequest().body(null);
            }
            Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());

            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());

            if (dto.getClockIn() != null && !dto.getClockIn().isEmpty()) {
                try {
                    attendance.setClockIn(LocalTime.parse(dto.getClockIn()));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(null);
                }
            }

            if (dto.getClockOut() != null && !dto.getClockOut().isEmpty()) {
                try {
                    attendance.setClockOut(LocalTime.parse(dto.getClockOut()));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(null);
                }
            }

            if (attendance.getClockIn() != null && attendance.getClockOut() != null) {
                validateAttendanceTimes(attendance.getClockIn(), attendance.getClockOut());

                attendance.setTotalHours(attendanceService.calculateTotalHours(
                        attendance.getClockIn(), attendance.getClockOut()));
                attendance.setOtHours(attendanceService.calculateOTHours(
                        attendance.getClockIn(), attendance.getClockOut(), LocalTime.of(16, 0)));
            } else {
                attendance.setTotalHours("0:00:00");
                attendance.setOtHours("0:00:00");
            }

            Attendance savedAttendance = attendanceService.saveAttendance(attendance);
            return ResponseEntity.ok(convertToDTO(savedAttendance));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.badRequest().body(null);
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

            if (dto.getEmployeeId() != null) {
                Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());
                attendance.setEmployee(employee);
            }

            if (dto.getDate() != null) {
                attendance.setDate(dto.getDate());
            }

            LocalTime clockIn = attendance.getClockIn();
            LocalTime clockOut = attendance.getClockOut();

            if (dto.getClockIn() != null && !dto.getClockIn().isEmpty()) {
                try {
                    clockIn = LocalTime.parse(dto.getClockIn());
                    attendance.setClockIn(clockIn);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(null);
                }
            }

            if (dto.getClockOut() != null && !dto.getClockOut().isEmpty()) {
                try {
                    clockOut = LocalTime.parse(dto.getClockOut());
                    attendance.setClockOut(clockOut);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(null);
                }
            }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long id) {
        try {
            attendanceService.deleteAttendance(id);
            return ResponseEntity.ok("Attendance deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Helper method to calculate break times for a list of attendances
    private List<AttendanceDTO> calculateBreakTimes(List<Attendance> attendances) {
        List<AttendanceDTO> attendanceDTOs = new ArrayList<>();

        for (int i = 0; i < attendances.size(); i++) {
            Attendance current = attendances.get(i);
            AttendanceDTO dto = convertToDTO(current);

            String breakTime = "00:00:00";
            if (current.getClockOut() != null && i + 1 < attendances.size()) {
                Attendance next = attendances.get(i + 1);
                // Only if next clockIn is after current clockOut and on the same day
                if (next.getDate().equals(current.getDate()) && next.getClockIn() != null &&
                    next.getClockIn().isAfter(current.getClockOut())) {
                    Duration breakDuration = Duration.between(current.getClockOut(), next.getClockIn());
                    long hours = breakDuration.toHours();
                    long minutes = breakDuration.toMinutes() % 60;
                    long seconds = breakDuration.getSeconds() % 60;
                    breakTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                }
            }
            dto.setBreakTime(breakTime);
            attendanceDTOs.add(dto);
        }
        return attendanceDTOs;
    }

    private void validateAttendanceTimes(LocalTime clockIn, LocalTime clockOut) {
        if (clockIn != null && clockOut != null) {
            Duration duration;
            if (clockOut.isBefore(clockIn)) {
                Duration firstPart = Duration.between(clockIn, LocalTime.MAX);
                Duration secondPart = Duration.between(LocalTime.MIN, clockOut);
                duration = firstPart.plus(secondPart).plusSeconds(1);
            } else {
                duration = Duration.between(clockIn, clockOut);
            }

            if (duration.toHours() > 16) {
                throw new IllegalArgumentException("Shift duration cannot exceed 16 hours");
            }

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
            dto.setAvatar(employee.getAvatar() != null ? employee.getAvatar() : "");

            // Set status based on attendance state
            if (attendance.getClockIn() != null && attendance.getClockOut() == null) {
                dto.setStatus("Active");  // Clocked in but not out
            } else if (attendance.getClockOut() != null) {
                dto.setStatus("Leave");   // Clocked out
            } else {
                dto.setStatus("Unknown"); // No clock-in info
            }
        } else {
            dto.setEmployeeId(null);
            dto.setEmployeeName("Unknown");
            dto.setRole("Unknown");
            dto.setStatus("Unknown");
            dto.setAvatar("");
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        dto.setClockIn(attendance.getClockIn() != null ? attendance.getClockIn().format(timeFormatter) : "");
        dto.setClockOut(attendance.getClockOut() != null ? attendance.getClockOut().format(timeFormatter) : "");
        dto.setTotalHours(attendance.getTotalHours() != null ? attendance.getTotalHours() : "0:00:00");
        dto.setOtHours(attendance.getOtHours() != null ? attendance.getOtHours() : "0:00:00");
        dto.setDate(attendance.getDate());
        dto.setBreakTime("00:00:00");

        return dto;
    }
}