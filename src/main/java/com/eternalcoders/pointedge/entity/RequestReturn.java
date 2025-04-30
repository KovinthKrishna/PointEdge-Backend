package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
public class RequestReturn {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String invoiceNumber;
    @Setter
    private LocalDateTime returnDate;
    @Setter
    private String refundMethod;
    @Setter
    private String reason;
    @Setter
    private double refundAmount;

    @Setter
    @OneToMany(mappedBy = "requestReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items;

}
