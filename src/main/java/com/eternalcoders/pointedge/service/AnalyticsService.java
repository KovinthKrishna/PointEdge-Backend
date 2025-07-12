package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.repository.OrderItemRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import com.eternalcoders.pointedge.repository.ReturnRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReturnRecordRepository returnRecordRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<CategoryDistributionDTO> getProductCategoryDistribution() {
        return productRepository.getProductCategoryDistribution();
    }

    public List<TopSellingProductDTO> getTopSellingProducts() {
        return orderItemRepository.findTopSellingProducts();
    }

    public List<DailySalesDTO> getSalesOverTime() {
        return orderItemRepository.getDailySales();
    }

    public List<ProductRevenueDTO> getRevenueByProduct() {
        return orderItemRepository.getRevenueByProduct();
    }

    public List<ProductReturnRateDTO> getReturnRatesByProduct() {
        return returnRecordRepository.getReturnRatesByProduct();
    }

}
