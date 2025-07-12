package com.eternalcoders.pointedge.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmployeeDashboardDTO {
    // Overview Stats
    private int totalEmployees;
    private int activeEmployees;
    private int onLeaveEmployees;
    private String totalHoursWorked;
    private int employeeChangePercent;
    private int hoursChangePercent;
    
    // Productivity Chart Data
    private List<MonthlyProductivity> productivityData;
    
    // Attendance Report Data
    private int activeCount;
    private int leaveCount;
    private int activePercentage;
    private int leavePercentage;
    
    // Weekly Attendance Data
    private List<DailyAttendance> weeklyAttendance;
    
    // Nested class for productivity data
    @Data
    public static class MonthlyProductivity {
        private String month;  // "Jan", "Feb", etc.
        private int primary;   // Regular hours
        private int secondary; // OT hours
    }
    
    // Nested class for daily attendance
    @Data
    public static class DailyAttendance {
        private String date;
        private String dayOfWeek;
        private int attendancePercentage;
        private int height; // For UI display scaling
    }
}