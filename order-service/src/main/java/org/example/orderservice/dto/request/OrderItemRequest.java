package org.example.orderservice.dto.request;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemRequest implements Serializable {
    private String productId;
    private Integer quantity;
}
