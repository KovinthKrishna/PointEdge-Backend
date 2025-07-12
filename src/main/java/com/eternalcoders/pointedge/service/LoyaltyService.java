package com.eternalcoders.pointedge.service;

import org.springframework.stereotype.Service;

@Service
public class LoyaltyService {
    public void updateLoyaltyPoints(Long customerId, double points) {
        System.out.println("Updating loyalty points for customer " + customerId + " to " + points + " points.");
    }
}