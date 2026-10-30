package org.example.orderservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.orderservice.entity.OrderItem;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCreateRequest implements Serializable {
    @NotNull
    private String customerId;

    @NotNull
    private List<OrderItemRequest> orderItemList;
}
