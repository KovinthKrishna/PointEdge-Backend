package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal Info
    @Column(nullable = false)
    private String name; // full name

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;

    // Auth Info
    @Setter
    private String tempPassword;

    // Role and avatar
    @Column(nullable = false)
    private String role;

    @Column(name = "avatar_url")
    private String avatar;
    
    @Column(name = "location")
    private String location;
    
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    // Shift / Attendance link
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attendance> attendances;

    // Status Enum
    public enum EmployeeStatus {
        Active, Leave
    }
}