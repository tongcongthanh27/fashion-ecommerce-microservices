package org.example.orderservice.mapper;

import org.example.orderservice.dto.request.OrderCreateRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = OrderItemMapper.class
)
public interface OrderMapper {

    Order toOrder(OrderCreateRequest request);

    @Mapping(
            target = "orderItemResponseList",
            source = "orderItems"
    )
    OrderResponse toOrderResponse(Order order);
}
