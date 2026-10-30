package org.example.orderservice.dto.response;

import lombok.*;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private String id;
    private String customerId;
    private String status;
    private Integer totalAmount;
    private List<OrderItemResponse> orderItemResponseList;
}
