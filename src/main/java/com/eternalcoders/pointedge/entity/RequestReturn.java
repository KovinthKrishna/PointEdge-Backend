package com.eternalcoders.pointedge.entity;

import com.eternalcoders.pointedge.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
public class RequestReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String refundMethod;

    private double totalRefundAmount;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private Employee createdBy;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @ManyToOne
    @JoinColumn(name = "invoice_id", referencedColumnName = "invoiceNumber")  // Fix here
    private Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(mappedBy = "requestReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items;
}