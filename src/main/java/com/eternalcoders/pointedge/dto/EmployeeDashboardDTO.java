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

    private long totalOrders;
    private double totalSales;
    
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
        private String month;        // "Jan", "Feb", etc.
        private int primary;         // Productivity percentage (0-100%)
        private int secondary;       // Total valid OT hours (max 4h per employee)
    }
    
    // Nested class for daily attendance
    @Data
    public static class DailyAttendance {
        private String date;                    // "2025-07-18"
        private String dayOfWeek;               // "Mon", "Tue", etc.
        private int attendancePercentage;       // 0-100%
        private int height;                     // For UI display scaling (percentage * 1.6)
    }
}