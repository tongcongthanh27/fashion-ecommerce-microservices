package org.example.orderservice.dto.response;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemResponse implements Serializable {
    private String id;
    private String productId;
    private String productName;
    private Integer quantity;
    private Integer price;
}
