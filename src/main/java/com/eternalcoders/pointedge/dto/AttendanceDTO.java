package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {

    private Long employeeId;
    private String employeeName;
    private String role;
    private String clockIn;
    private String clockOut;
    private String totalHours;
    private String otHours;
    private String status;
    private String avatar;
    private LocalDate date;
}

