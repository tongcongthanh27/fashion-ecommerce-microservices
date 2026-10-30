package org.example.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.dto.client.request.ProductFilter;
import org.example.orderservice.dto.client.request.StockDeductRequest;
import org.example.orderservice.dto.client.request.StockItemRequest;
import org.example.orderservice.dto.client.response.ProductDTO;
import org.example.orderservice.dto.request.OrderCreateRequest;
import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.exception.AppException;
import org.example.orderservice.exception.ErrorCode;
import org.example.orderservice.mapper.OrderItemMapper;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        List<String> productIds = request.getOrderItemList().stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .toList();
        List<ProductDTO> products = productClient.getProductsByIds(new ProductFilter(productIds));
        Map<String, ProductDTO> productPriceMap = new HashMap<>();
        products.forEach(productDTO -> {
            productPriceMap.put(productDTO.getId(), productDTO);
        });

        Order order = orderMapper.toOrder(request);
        List<OrderItem> orderItems = new ArrayList<>();
        List<StockItemRequest> stockItemRequests = new ArrayList<>();
        Integer total_amount = 0;
        for (OrderItemRequest request1: request.getOrderItemList()){
            ProductDTO productDTO = productPriceMap.get(request1.getProductId());
            if (productDTO == null){
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (request1.getQuantity() > productDTO.getStock()){
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            total_amount += productDTO.getPrice()* request1.getQuantity();
            OrderItem orderItem = orderItemMapper.toOrderItem(request1);
            orderItem.setOrder(order);
            orderItems.add(orderItem);
            orderItem.setPrice(productDTO.getPrice());

            StockItemRequest request2 = new StockItemRequest();
            request2.setProductId(productDTO.getId());
            request2.setQuantity(request1.getQuantity());
            stockItemRequests.add(request2);
        }
        order.setStatus("NEW");
        order.setTotalAmount(total_amount);
        order.setOrderItems(orderItems);
        orderRepository.save(order);
        StockDeductRequest stockDeductRequest = new StockDeductRequest();
        stockDeductRequest.setStockItemRequests(stockItemRequests);
        productClient.updateQuantity(stockDeductRequest);
        return orderMapper.toOrderResponse(order);
    }
}
