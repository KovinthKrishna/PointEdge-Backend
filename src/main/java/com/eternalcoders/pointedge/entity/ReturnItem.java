package com.eternalcoders.pointedge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.eternalcoders.pointedge.entity.RequestReturn;


@Getter
@Entity
public class ReturnItem {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private Long productId;
    @Setter
    private int quantity;

    @Setter
    private double refundAmount;


    @Setter
    @ManyToOne
    @JoinColumn(name = "request_return_id")
    private RequestReturn requestReturn;


}