package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.entity.OrderItem;
import com.eternalcoders.pointedge.repository.OrderRepository;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
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
}
