package com.eternalcoders.pointedge.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "employees")
@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;

    @Column(nullable = false)
    private String name;

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
    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attendance> attendances;

    public enum EmployeeStatus {
        Active, Leave
    }
}