package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    
    @Id
    
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String role;
    
    @Column(name = "avatar_url")
    private String avatar;
    
    @Column(name = "location")
    private String location;
    
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;
    
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<Attendance> attendances;
    
    public enum EmployeeStatus {
        Active, Leave
    }
}