package org.example.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.dto.client.request.ProductFilter;
import org.example.orderservice.dto.client.request.StockDeductRequest;
import org.example.orderservice.dto.client.request.StockItemRequest;
import org.example.orderservice.dto.client.response.ProductDTO;
import org.example.orderservice.dto.request.OrderCreateRequest;
import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.response.OrderItemResponse;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.events.OrderCreatedEvent;
import org.example.orderservice.exception.AppException;
import org.example.orderservice.exception.ErrorCode;
import org.example.orderservice.mapper.OrderItemMapper;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.repository.OrderItemRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        List<String> productIds = request.getOrderItemList()
                .stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .toList();
        List<ProductDTO> products = productClient.getProductsByIds(
                new ProductFilter(productIds)
        );
        Map<String, ProductDTO> productPriceMap = new HashMap<>();
        products.forEach(product ->
                productPriceMap.put(product.getId(), product)
        );
        Order order = orderMapper.toOrder(request);
        List<OrderItem> orderItems = new ArrayList<>();
        int totalAmount = 0;
        for (OrderItemRequest itemRequest : request.getOrderItemList()) {
            ProductDTO productDTO = productPriceMap.get(itemRequest.getProductId());
            if (productDTO == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity");
            }
            if (itemRequest.getQuantity() > productDTO.getStock()) {
                throw new IllegalArgumentException("Insufficient stock");
            }
            totalAmount = Math.addExact(
                    totalAmount,
                    Math.multiplyExact(
                            productDTO.getPrice(),
                            itemRequest.getQuantity()
                    )
            );
            OrderItem orderItem = orderItemMapper.toOrderItem(itemRequest);
            orderItem.setOrder(order);
            orderItem.setPrice(productDTO.getPrice());
            orderItems.add(orderItem);
        }
        order.setStatus("NEW");
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        OrderCreatedEvent orderCreatedEvent =
                orderMapper.toEvent(order);
        orderCreatedEvent.setOrderItems(orderItems);
        kafkaTemplate.send("order_created", orderCreatedEvent);
        log.info("Requested Kafka publish for orderId: {}", order.getId());
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> {
                    OrderItemResponse response =
                            orderItemMapper.toOrderItemResponse(item);

                    ProductDTO productDTO =
                            productPriceMap.get(item.getProductId());
                    if (productDTO != null) {
                        response.setProductName(productDTO.getName());
                    }
                    return response;
                })
                .toList();
        orderResponse.setOrderItemResponseList(itemResponses);
        return orderResponse;
    }

    @Override
    @Transactional
    public void updateStatus(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()
               -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        order.setStatus("PREPARED");
    }

}
