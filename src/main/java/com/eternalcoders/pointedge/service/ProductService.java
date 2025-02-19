package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.OrderItemRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    public List<ProductOrderQuantityDTO> getFilteredProductOrderQuantities(
            Long brandId, Long categoryId, String timeFilter) {

        LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startDate = null;
        LocalDateTime endDate = switch (timeFilter == null ? "" : timeFilter) {
            case "TODAY" -> {
                startDate = todayStart;
                yield todayStart.plusDays(1);
            }
            case "YESTERDAY" -> {
                startDate = todayStart.minusDays(1);
                yield todayStart;
            }
            case "THIS_WEEK" -> {
                startDate = todayStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield startDate.plusWeeks(1);
            }
            case "THIS_MONTH" -> {
                startDate = todayStart.withDayOfMonth(1);
                yield startDate.plusMonths(1);
            }
            case "THIS_YEAR" -> {
                startDate = todayStart.withDayOfYear(1);
                yield startDate.plusYears(1);
            }
            default -> null;
        };

        return orderItemRepository.getProductOrderQuantities(brandId, categoryId, startDate, endDate);
    }
}
