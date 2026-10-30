package org.example.micro.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductResponse {

    private String id;
    private String name;
    private Integer price;
    private Integer stock;
    private String categoryId;
    private String categoryName;
}
