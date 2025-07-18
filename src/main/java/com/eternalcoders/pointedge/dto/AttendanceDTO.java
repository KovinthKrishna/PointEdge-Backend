package com.eternalcoders.pointedge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    // Mark these properties as read-only for JSON deserialization
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String totalHours;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String otHours;
    
    private String status;
    private String avatar;
    private LocalDate date;
    private String breakTime;
    public String getBreakTime() { return breakTime; }
    public void setBreakTime(String breakTime) { this.breakTime = breakTime; }
}