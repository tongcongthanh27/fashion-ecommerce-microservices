package org.example.micro.consumers.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreatedEvent extends Order {
    List<OrderItem> orderItems;
}
