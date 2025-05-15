package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    public Object getEmail() {
        return this.email;
    }
    
    public Object getName() {
        return this.name;
    }
    
    public Object getPoints() {
        return this.points;
    }
    
    public Object getTier() {
        return this.tier;
    }
    
    public Object getTitle() {
        return this.title;
    }
    

    public enum Title {
        MR,
        MRS,
        OTHER;

        int toUpperCase() {
            throw new UnsupportedOperationException("Unimplemented method 'toUpperCase'");
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "title", nullable = false)
    private Title title;

    @Column(name = "email", unique = true, nullable = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "points", nullable = false)
    private Double points;

    public enum Tier {
        GOLD,
        SILVER,
        BRONZE,
        NOTLOYALTY
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private Tier tier;

}