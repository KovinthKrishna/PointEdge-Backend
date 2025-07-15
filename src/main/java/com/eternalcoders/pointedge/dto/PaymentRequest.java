package com.eternalcoders.pointedge.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String customerEmail;  // Optional for receipt
    private Long amount;           // Amount in cents(Stripe standard)
    private String currency;       // e.g., "lkr" or "usd"
    private String paymentMethodId; // Stripe PaymentMethod ID (received from frontend)
}