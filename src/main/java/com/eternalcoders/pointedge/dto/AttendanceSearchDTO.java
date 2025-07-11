package com.eternalcoders.pointedge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSearchDTO {
    private String date;
    private String startTime;
    private String endTime;
    private String searchQuery;
    private Long employeeId;
}