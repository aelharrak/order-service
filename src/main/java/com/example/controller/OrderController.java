package com.example.controller;

import com.example.model.Order;
import com.example.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest request) throws Exception {
        UUID orderId = UUID.randomUUID();
        orderService.createOrder(orderId, request.status(), request.totalAmount());
        return ResponseEntity.ok("Order created with ID: " + orderId);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID orderId) throws Exception {
        Order order = orderService.reconstructOrder(orderId);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    public record OrderRequest(String status, double totalAmount) {
    }
}