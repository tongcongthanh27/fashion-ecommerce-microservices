package org.example.orderservice.mapper;

import lombok.Builder;
import org.example.orderservice.dto.request.OrderCreateRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.events.OrderCreatedEvent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(
        componentModel = "spring",
        uses = OrderItemMapper.class
)
public interface OrderMapper {
    Order toOrder(OrderCreateRequest request);
    OrderResponse toOrderResponse(Order order);
    @BeanMapping(builder = @org.mapstruct.Builder(disableBuilder = true))
    OrderCreatedEvent toEvent(Order order);
}
