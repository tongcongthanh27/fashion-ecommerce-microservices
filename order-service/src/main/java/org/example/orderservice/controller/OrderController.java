package org.example.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.consumers.dto.ProductLockedEvent;
import org.example.orderservice.dto.ApiResponse;
import org.example.orderservice.dto.request.OrderCreateRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request){
        return ApiResponse.<OrderResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo order thành công")
                .result(orderService.createOrder(request))
                .build();
    }

    @PutMapping
    public ApiResponse<Void> updateStatus(String orderId){
        orderService.updateStatus(orderId);
        return ApiResponse.<Void>builder()
                .message("Cap nhat status thanh cong")
                .build();
    }
}
