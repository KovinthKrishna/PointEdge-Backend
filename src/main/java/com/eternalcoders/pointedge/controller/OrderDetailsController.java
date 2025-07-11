package com.eternalcoders.pointedge.controller;

import java.util.Map;
import com.eternalcoders.pointedge.service.OrderDetailsService;
import com.eternalcoders.pointedge.service.CustomerService;
import com.eternalcoders.pointedge.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/discount/analytics")
public class OrderDetailsController {
    
    @Autowired
    private OrderDetailsService orderDetailsService;
    
    @Autowired
    private CustomerService customerService;

    @Autowired
    private DiscountService discountService;

    // count orders by time slot  
    @GetMapping("/orders/count")
    public ResponseEntity<Map<String, Long>> getOrderCounts() {
        return ResponseEntity.ok(orderDetailsService.getOrderCounts());
    }

    // count discounts by time slot
    @GetMapping("/discounts/count-by-type")
    public ResponseEntity<Map<String, Map<String, Long>>> getDiscountCountsByType() {
        return ResponseEntity.ok(orderDetailsService.getDiscountCountsByType());
    }

    //count customers by time slot
    @GetMapping("/customers/count-by-tier")
    public ResponseEntity<Map<String, Object>> getCustomerCountsByTier() {
        return ResponseEntity.ok(orderDetailsService.getCustomerCountsByTier());
    }

    // count total loyalty discount
    @GetMapping("/loyalty-discounts/by-tier")
    public ResponseEntity<Map<String, Object>> getLoyaltyDiscountDataByTier() {
        return ResponseEntity.ok(orderDetailsService.getLoyaltyDiscountDataByTier());
    }

    // amount and top 3 items
    @GetMapping("/item-discounts/analytics")
    public ResponseEntity<Map<String, Object>> getItemDiscountAnalytics() {
        return ResponseEntity.ok(orderDetailsService.getItemDiscountAnalytics());
    }

    // category discounts
    @GetMapping("/category-discounts/analytics")
    public ResponseEntity<Map<String, Object>> getCategoryDiscountAnalytics() {
        return ResponseEntity.ok(orderDetailsService.getCategoryDiscountAnalytics());
    }

    // get total discount
    @GetMapping("/discounts/totals")
    public ResponseEntity<Map<String, Object>> getAllDiscountTotals() {
        return ResponseEntity.ok(orderDetailsService.getAllDiscountTotals());
    }

    // total amount
    @GetMapping("/orders/total-amount")
    public ResponseEntity<Map<String, Object>> getOrderSummaryMetrics() {
        return ResponseEntity.ok(orderDetailsService.getOrderSummaryMetrics());
    }

}