package org.example.orderservice.mapper;

import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    OrderItem toOrderItem(OrderItemRequest request);
}
