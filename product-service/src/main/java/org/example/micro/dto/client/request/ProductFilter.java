package org.example.micro.dto.client.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductFilter {
    @NotNull
    private List<String> ids;
}
