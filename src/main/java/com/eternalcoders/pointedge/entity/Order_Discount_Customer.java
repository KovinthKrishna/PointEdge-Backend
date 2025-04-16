package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_discount_customer")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Order_Discount_Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "order_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_odc_order"),
        nullable = false
    )
    private Order order; 

    @ManyToOne
    @JoinColumn(
        name = "discount_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_odc_discount"),
        nullable = false
    )
    private Discount discount;

    @ManyToOne
    @JoinColumn(
        name = "customer_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_odc_customer"),
        nullable = false
    )
    private Customer customer; 

    @Column(name = "datetime", nullable = false)
    private LocalDateTime datetime;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "item_discount")
    private Double itemDiscount;

    @Column(name = "category_discount")
    private Double categoryDiscount;

    @Column(name = "loyalty_discount")
    private Double loyaltyDiscount;
}