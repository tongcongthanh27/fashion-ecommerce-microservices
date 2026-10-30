package org.example.orderservice.service;

import org.example.orderservice.dto.request.OrderCreateRequest;
import org.example.orderservice.dto.response.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request);
}
