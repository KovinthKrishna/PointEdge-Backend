package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(nullable = false)
    private LocalDateTime transactionDateTime;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private Integer itemCount;
    
    // Order identifier - can be used to group multiple transactions as one order
    @Column(name = "order_id", nullable = false)
    private String orderId;
}