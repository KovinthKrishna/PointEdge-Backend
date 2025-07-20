package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.OrderRequestDTO;
import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.entity.OrderItem;
import com.eternalcoders.pointedge.repository.OrderItemRepository;
import com.eternalcoders.pointedge.repository.OrderRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InvoiceService invoiceService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository, InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.invoiceService = invoiceService;
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
            Long brandId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable) {
        LocalDateTime startDateWithTime = null;
        LocalDateTime endDateWithTime = null;

        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            startDateWithTime = startDate.atStartOfDay();
            endDateWithTime = endDate.atTime(LocalTime.MAX);
        }

        return orderItemRepository.getTotalOrdersForProducts(
                brandId,
                categoryId,
                startDateWithTime,
                endDateWithTime,
                search,
                pageable
        );
    }

    @Transactional
    public Order createOrderWithInvoice(OrderRequestDTO dto) {
        var order = new Order();

        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setLoyaltyPoints(dto.getLoyaltyPoints());
        order.setDiscountCode(dto.getDiscountCode());

        order.setAmount(dto.getAmount());
        order.setTotalDiscount(dto.getTotalDiscount());
        order.setTotal(dto.getTotal());

        order.setEmployeeId(dto.getEmployeeId());
        order.setCashierName(dto.getCashierName());

        List<OrderItem> orderItems = dto.getItems().stream().map(itemDTO -> {
            var product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemDTO.getProductId()));

            var orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPricePerUnit(itemDTO.getPricePerUnit());
            orderItem.setOrder(order);
            productRepository.reduceStock(product.getId(), itemDTO.getQuantity());
            return orderItem;
        }).toList();

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        invoiceService.createInvoiceFromOrder(savedOrder);

        return savedOrder;
    }
}
