package org.example.micro.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.micro.consumers.dto.OrderCreatedEvent;
import org.example.micro.dto.client.request.StockDeductRequest;
import org.example.micro.dto.client.request.StockItemRequest;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.RetryableTopic;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.micro.consumers.dto.Order;
import org.example.micro.service.ProductService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order_created")
    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000, multiplier = 2.0),
            exclude = {NullPointerException.class, OutOfMemoryError.class, IllegalAccessError.class}

    )
    public void handleOrderCreatedEvent(String message) throws JsonProcessingException {
        OrderCreatedEvent orderCreatedEvent = objectMapper.readValue(message, OrderCreatedEvent.class);
        log.info("Received Kafka message: {}", orderCreatedEvent);

        List<StockItemRequest> stockItemRequestList = new ArrayList<>();
        orderCreatedEvent.getOrderItems().forEach(orderItem -> {
            StockItemRequest stockItemRequest = new StockItemRequest();
            stockItemRequest.setProductId(orderItem.getProductId());
            stockItemRequest.setQuantity(orderItem.getQuantity());
            stockItemRequestList.add(stockItemRequest);
        });

        StockDeductRequest stockDeductRequest = new StockDeductRequest();
        stockDeductRequest.setOrderId(orderCreatedEvent.getId());
        stockDeductRequest.setStockItemRequests(stockItemRequestList);

        productService.updateStock(stockDeductRequest);
        log.info("Success to lock product item of {}", orderCreatedEvent.getId());
    }
}
