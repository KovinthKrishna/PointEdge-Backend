package com.eternalcoders.pointedge.entity;
import com.eternalcoders.pointedge.entity.Product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Entity
public class ReturnRecord {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String invoiceNumber;
    @Setter
    private Long invoiceItemId;
    @Setter
    private Long productId;
    @Setter
    private int quantityReturned;
    @Setter
    private String reason;
    @Setter
    private String refundMethod;
    @Setter
    private LocalDateTime returnedAt;

    @Setter
    @ManyToOne
    @JoinColumn(name = "replacement_product_id")
    private Product replacementProduct;

}
