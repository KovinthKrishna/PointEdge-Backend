package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Invoice {
    @Id
    private String invoiceNumber;

    private LocalDateTime date;
    private Double totalAmount;
    @Setter
    @Getter
    private Integer loyaltyPoints;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    // OneToMany: Invoice has many InvoiceItems
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceItem> items;

}

