package com.eternalcoders.pointedge.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private boolean success;
    private String message;
    private String transactionId;
    private String receiptUrl;
    private String clientSecret;

    public void setPaymentIntentId(String id) {
        this.transactionId = id;
    }
}