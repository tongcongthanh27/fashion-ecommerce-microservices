package org.example.orderservice.dto.client.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockDeductRequest {
    List<StockItemRequest> stockItemRequests;
}
