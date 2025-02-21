package com.eternalcoders.pointedge.controller;

import com.eternalcoders.pointedge.dto.ProductOrderQuantityDTO;
import com.eternalcoders.pointedge.entity.Order;
import com.eternalcoders.pointedge.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    public ResponseEntity<Order> addOrder(@RequestBody Order order) {
        return ResponseEntity.status(201).body(orderService.addOrder(order));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<ProductOrderQuantityDTO>> getTotalOrdersForProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String timeFilter
    ) {
        return ResponseEntity.ok(orderService.getTotalOrdersForProducts(brandId, categoryId, timeFilter));
    }
}
