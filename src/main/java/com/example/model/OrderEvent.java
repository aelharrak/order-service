package com.example.model;

import java.util.UUID;

public record OrderEvent(UUID orderId, String status, double totalAmount) {
}