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

    private Long productId;
    private String productName;
    private Integer quantity;
    private Double price;
    private boolean returned = false;

    @ManyToOne
    @JoinColumn(name = "invoice_number")
    private Invoice invoice;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
