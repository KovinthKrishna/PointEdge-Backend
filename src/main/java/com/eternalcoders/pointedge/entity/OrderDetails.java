package com.eternalcoders.pointedge.entity;

import com.eternalcoders.pointedge.entity.Customer;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "order_details")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class OrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "customer_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_odc_customer"),
        nullable = false
    )
    private Customer customer;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "discount_id")
    private Long discountId;

    @Column(name = "datetime", nullable = false)
    @Builder.Default
    private LocalDateTime datetime = LocalDateTime.now();

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "total_discount", nullable = false)
    private Double totalDiscount;

    @Column(name = "item_discount")
    private Double itemDiscount;

    @Column(name = "category_discount")
    private Double categoryDiscount;

    @Column(name = "loyalty_discount")
    private Double loyaltyDiscount;

    @Column(name = "loyalty_tier")
    private String loyaltyTier;

    @Column(name = "points_earned")
    private Double pointsEarned;

}