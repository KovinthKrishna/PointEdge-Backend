package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.service.DiscountCalculatorService;
import com.eternalcoders.pointedge.service.DiscountService;
import com.eternalcoders.pointedge.service.DiscountCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/discounts")
public class DiscountCalculatorController {

    @Autowired
    private DiscountCalculatorService discountCalculatorService;

    /**
     * Calculate discounts for an order
     * @param orderData Order items with quantities
     * @param customerId Optional customer ID for loyalty discounts
     * @return Discount calculation result
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateDiscounts(
            @RequestBody DiscountCalculatorService.OrderData orderData,
            @RequestParam(required = false) Long customerId) {
        
        try {
            DiscountCalculatorService.DiscountResult result = discountCalculatorService.calculateDiscounts(orderData, customerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to calculate discounts: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Apply calculated discounts and save to database
     * @param discountResult Pre-calculated discount result
     * @param customerId Optional customer ID
     * @return Updated discount result with order ID
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyDiscounts(
            @RequestBody DiscountCalculatorService.DiscountResult discountResult,
            @RequestParam(required = false) Long customerId) {
            
        try {
            DiscountCalculatorService.DiscountResult appliedResult = discountCalculatorService.applyDiscount(discountResult, customerId);
            return ResponseEntity.ok(appliedResult);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to apply discounts: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}