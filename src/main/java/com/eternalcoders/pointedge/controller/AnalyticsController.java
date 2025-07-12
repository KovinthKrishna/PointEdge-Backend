package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/top-selling-products")
    public List<TopSellingProductDTO> getTopSellingProducts() {
        return analyticsService.getTopSellingProducts();
    }

    @GetMapping("/sales-over-time")
    public List<DailySalesDTO> getSalesOverTime() {
        return analyticsService.getSalesOverTime();
    }

    @GetMapping("/revenue")
    public List<ProductRevenueDTO> getRevenueByProduct() {
        return analyticsService.getRevenueByProduct();
    }

    @GetMapping("/return-rates")
    public List<ProductReturnRateDTO> getReturnRatesByProduct() {
        return analyticsService.getReturnRatesByProduct();
    }

    @GetMapping("/category-distribution")
    public List<CategoryDistributionDTO> getCategoryDistribution() {
        return analyticsService.getProductCategoryDistribution();
    }
}