package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.OrderRequestDTO;
import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.entity.OrderItem;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.OrderItemRepository;
import com.eternalcoders.pointedge.repository.OrderRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public Order addOrder(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItem.setOrder(order);
            productRepository.reduceStock(orderItem.getProduct().getId(), orderItem.getQuantity());
        }
        return orderRepository.save(order);
    }

    public Page<ProductOrderQuantityDTO> getTotalOrdersForProducts(
            Long brandId, Long categoryId, String timeFilter, String search, Pageable pageable) {
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
        return orderItemRepository.getTotalOrdersForProducts(brandId, categoryId, startDate, endDate, search, pageable);
    }

    @Transactional
    public Order createOrderFromDTO(OrderRequestDTO dto) {
        Order order = new Order();
        order.setOrderDate(dto.getOrderDate());

        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setLoyaltyPoints(dto.getLoyaltyPoints());
        order.setDiscountCode(dto.getDiscountCode());

        order.setAmount(dto.getAmount());
        order.setTotalDiscount(dto.getTotalDiscount());
        order.setTotal(dto.getTotal());
        order.setCashierName(dto.getCashierName());
        order.setEmployeeId(dto.getEmployeeId());
        List<OrderItem> orderItems = dto.getItems().stream().map(itemDTO -> {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemDTO.getProductId()));
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPricePerUnit(itemDTO.getPricePerUnit());
            orderItem.setOrder(order);
            return orderItem;
        }).toList();

        order.setOrderItems(orderItems);

        return orderRepository.save(order);
    }
}
