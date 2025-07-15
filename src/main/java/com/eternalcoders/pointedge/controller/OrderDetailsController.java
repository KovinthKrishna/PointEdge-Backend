package com.eternalcoders.pointedge.controller;

import java.time.LocalDateTime;
import java.util.Map;
import com.eternalcoders.pointedge.service.OrderDetailsService;
import com.eternalcoders.pointedge.service.CustomerService;
import com.eternalcoders.pointedge.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

    // Add these methods to OrderDetailsController.java

@GetMapping("/orders/count/custom")
public ResponseEntity<Map<String, Long>> getOrderCountsByCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getOrderCountsByCustomRange(startDate, endDate));
}

@GetMapping("/discounts/count-by-type/custom")
public ResponseEntity<Map<String, Map<String, Long>>> getDiscountCountsByTypeForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getDiscountCountsByTypeForCustomRange(startDate, endDate));
}

@GetMapping("/customers/count-by-tier/custom")
public ResponseEntity<Map<String, Object>> getCustomerCountsByTierForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getCustomerCountsByTierForCustomRange(startDate, endDate));
}

@GetMapping("/loyalty-discounts/by-tier/custom")
public ResponseEntity<Map<String, Object>> getLoyaltyDiscountDataByTierForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getLoyaltyDiscountDataByTierForCustomRange(startDate, endDate));
}

@GetMapping("/item-discounts/analytics/custom")
public ResponseEntity<Map<String, Object>> getItemDiscountAnalyticsForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getItemDiscountAnalyticsForCustomRange(startDate, endDate));
}

@GetMapping("/category-discounts/analytics/custom")
public ResponseEntity<Map<String, Object>> getCategoryDiscountAnalyticsForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getCategoryDiscountAnalyticsForCustomRange(startDate, endDate));
}

@GetMapping("/discounts/totals/custom")
public ResponseEntity<Map<String, Object>> getAllDiscountTotalsForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getAllDiscountTotalsForCustomRange(startDate, endDate));
}

@GetMapping("/orders/total-amount/custom")
public ResponseEntity<Map<String, Object>> getOrderSummaryMetricsForCustomRange(
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    return ResponseEntity.ok(orderDetailsService.getOrderSummaryMetricsForCustomRange(startDate, endDate));
}

}