package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.dto.OrderRequestDTO;
import com.eternalcoders.pointedge.dto.OrderStatsDTO;
import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.entity.OrderItem;
import com.eternalcoders.pointedge.exception.InsufficientStockException;
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
import java.util.ArrayList;
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
        for (OrderItem item : order.getOrderItems()) {
            int updated = productRepository.reduceStock(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
            if (updated == 0) {
                throw new InsufficientStockException(
                        item.getProduct().getName()
                                + " only has " + item.getProduct().getStockQuantity()
                                + " items left, cannot fulfill quantity of "
                                + item.getQuantity()
                );
            }
            item.setOrder(order);
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
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atTime(LocalTime.MAX);
        }

        return orderItemRepository.getTotalOrdersForProducts(
                brandId,
                categoryId,
                startDateTime,
                endDateTime,
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

        List<OrderItem> items = new ArrayList<>();
        for (var itemDTO : dto.getItems()) {
            var product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found with ID: " + itemDTO.getProductId()
                    ));

            int updated = productRepository.reduceStock(
                    product.getId(),
                    itemDTO.getQuantity()
            );
            if (updated == 0) {
                throw new InsufficientStockException(
                        "Cannot order " + itemDTO.getQuantity()
                                + " of product " + product.getName()
                                + " (only " + product.getStockQuantity() + " left)"
                );
            }

            var oi = new OrderItem();
            oi.setProduct(product);
            oi.setQuantity(itemDTO.getQuantity());
            oi.setPricePerUnit(itemDTO.getPricePerUnit());
            oi.setOrder(order);
            items.add(oi);
        }
        order.setOrderItems(items);

        Order saved = orderRepository.save(order);
        invoiceService.createInvoiceFromOrder(saved);

        return saved;
    }

    public OrderStatsDTO getOrderStats(
            Long brandId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }

        return orderRepository.findOrderStats(
                brandId, categoryId, startDateTime, endDateTime
        );
    }
}
