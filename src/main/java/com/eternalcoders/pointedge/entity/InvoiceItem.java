package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class InvoiceItem {
    @Setter
    @Getter
    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @Getter
    private Long productId;
    private String productName;
    @Setter
    @Getter
    private Integer quantity;
    @Setter
    @Getter
    private Double price;
    private boolean returned = false;

    @ManyToOne
    @JoinColumn(name = "invoice_number", referencedColumnName = "invoiceNumber")
    private Invoice invoice;

}
