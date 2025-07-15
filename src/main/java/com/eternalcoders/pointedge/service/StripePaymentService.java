package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.PaymentRequest;
import com.eternalcoders.pointedge.dto.PaymentResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService implements PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        PaymentResponse response = new PaymentResponse();
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount())
                    .setCurrency(request.getCurrency())
                    .setReceiptEmail(request.getCustomerEmail())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            response.setSuccess(true);
            response.setMessage("PaymentIntent created successfully.");
            response.setTransactionId(paymentIntent.getId());
            response.setClientSecret(paymentIntent.getClientSecret());

        } catch (StripeException e) {
            response.setSuccess(false);
            response.setMessage("PaymentIntent creation failed: " + e.getMessage());
        }

        return response;
    }
}
