package com.example.model;

import java.util.UUID;

public record Order(UUID orderId, String status, double totalAmount) {
}