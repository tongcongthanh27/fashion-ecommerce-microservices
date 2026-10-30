package org.example.orderservice.dto.client.response;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO implements Serializable {
    private String id;
    private String name;
    private Integer price;
    private Integer stock;
    private String categoryId;
}
