package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int quantity;
    private double refundAmount;
    private double unitPrice;
    private String reason;
    private String photoPath;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "invoice_item_id")
    private InvoiceItem invoiceItem;

    @ManyToOne
    @JoinColumn(name = "request_return_id")
    private RequestReturn requestReturn;
}