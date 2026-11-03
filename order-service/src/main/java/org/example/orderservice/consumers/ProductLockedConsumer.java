package org.example.orderservice.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.consumers.dto.ProductLockedEvent;
import org.example.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLockedConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(topics = "lock_product")
    public void handleProductLockedConsumer(String message) throws JsonProcessingException {
        ProductLockedEvent productLockedEvent = objectMapper.readValue(message, ProductLockedEvent.class);
        log.info("receiced kafka message {}", productLockedEvent);

        orderService.updateStatus(productLockedEvent.getOrderId());
        log.info("Success to update order {}", productLockedEvent.getOrderId());
    }
}
