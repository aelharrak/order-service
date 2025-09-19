package com.example.service;

import com.example.model.Order;
import com.example.model.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OrderService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void createOrder(UUID orderId, String status, double totalAmount) throws Exception {
        String eventType = "OrderCreated";
        String payload = objectMapper.writeValueAsString(
                new OrderEvent(orderId, status, totalAmount)
        );

        jdbcTemplate.update(
                "INSERT INTO events (aggregate_id, event_type, payload) VALUES (?, ?, ?::jsonb)",
                orderId, eventType, payload
        );

        jdbcTemplate.update(
                "INSERT INTO orders (order_id, status, total_amount) VALUES (?, ?, ?)",
                orderId, status, totalAmount
        );
    }

    public Order reconstructOrder(UUID orderId) throws Exception {
        var events = jdbcTemplate.query(
                "SELECT event_type, payload FROM events WHERE aggregate_id = ? ORDER BY created_at",
                (rs, rowNum) -> new Event(rs.getString("event_type"), rs.getString("payload")),
                orderId
        );

        Order order = null;
        for (Event event : events) {
            if ("OrderCreated".equals(event.eventType())) {
                OrderEvent orderEvent = objectMapper.readValue(event.payload(), OrderEvent.class);
                order = new Order(orderEvent.orderId(), orderEvent.status(), orderEvent.totalAmount());
            }
        }
        return order;
    }

    private record Event(String eventType, String payload) {
    }
}