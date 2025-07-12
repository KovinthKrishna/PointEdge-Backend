package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class RequestReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Invoice invoice;

    @OneToMany(mappedBy = "requestReturn", cascade = CascadeType.ALL)
    private List<ReturnItem> items;

    private String refundMethod;
    private double totalRefundAmount;
    private LocalDateTime createdAt;
}