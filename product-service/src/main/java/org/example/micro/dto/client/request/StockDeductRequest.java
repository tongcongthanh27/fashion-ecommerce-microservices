package org.example.micro.dto.client.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockDeductRequest {
    private String orderId;
    private List<StockItemRequest> stockItemRequests;
}