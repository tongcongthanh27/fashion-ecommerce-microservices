package org.example.micro.consumers.dto;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private String id;
    private String order;
    private String productId;
    private Integer price;
    private Integer quantity;
}