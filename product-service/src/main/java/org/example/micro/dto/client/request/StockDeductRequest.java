package org.example.micro.dto.client.request;

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