package org.example.orderservice.consumers.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLockedEvent {
    private String orderId;
}