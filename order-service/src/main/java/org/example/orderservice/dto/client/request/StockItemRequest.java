package org.example.orderservice.dto.client.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItemRequest {
    private String productId;
    private Integer quantity;
}