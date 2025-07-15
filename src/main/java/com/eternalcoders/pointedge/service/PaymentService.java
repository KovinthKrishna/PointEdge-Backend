package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.PaymentRequest;
import com.eternalcoders.pointedge.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest paymentRequest);
}
