package com.eternalcoders.pointedge.entity;

import com.eternalcoders.pointedge.enums.RequestStatus;
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

    // New fields for admin review
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private LocalDateTime reviewedAt;

    private String reviewedBy; // Can be an admin username or ID
}