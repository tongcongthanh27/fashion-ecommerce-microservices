package org.example.orderservice.events;

import lombok.*;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;

import java.util.List;

@Getter
@Setter
public class OrderCreatedEvent extends Order {
    List<OrderItem> orderItems;
}
