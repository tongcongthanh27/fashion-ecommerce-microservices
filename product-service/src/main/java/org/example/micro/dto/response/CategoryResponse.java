package org.example.micro.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResponse {
    private String id;
    private String name;
    private String parentId;
}
