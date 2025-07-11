package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class InvoiceItem {

    @Id
    @GeneratedValue
    private Long id;

    private Long productId; // Still needed for stock updates
    private String productName;
    private Integer quantity;
    private Double price;
    private boolean returned = false;

    @ManyToOne
    @JoinColumn(name = "invoice_number", referencedColumnName = "invoiceNumber")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    // --- New dynamic getters ---

    public String getProductName() {
        return orderItem != null ? orderItem.getProduct().getName() : productName;
    }

    public Long getProductId() {
        return orderItem != null ? orderItem.getProduct().getId() : productId;
    }

    public Double getPrice() {
        return orderItem != null ? orderItem.getPricePerUnit() : price;
    }
}
