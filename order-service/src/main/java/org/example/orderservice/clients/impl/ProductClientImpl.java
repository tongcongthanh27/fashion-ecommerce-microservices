package org.example.orderservice.clients.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.dto.ApiResponse;
import org.example.orderservice.dto.client.request.ProductFilter;
import org.example.orderservice.dto.client.request.StockDeductRequest;
import org.example.orderservice.dto.client.response.ProductDTO;
import org.example.orderservice.exception.AppException;
import org.example.orderservice.exception.ErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<ProductDTO> getProductsByIds(ProductFilter productFilter) {
        ApiResponse<List<ProductDTO>> response = webClientBuilder.build()
                .post()
                .uri("http://localhost:6000/api/products/search")
                .bodyValue(productFilter)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ProductDTO>>>() {
                })
                .block();
        if (response == null || response.getResult() == null){
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response.getResult();
    }

    @Override
    public void updateQuantity(StockDeductRequest request) {
        ApiResponse<Void> response = webClientBuilder.build()
                .put()
                .uri("http://localhost:6000/api/products/updateStock")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(
                        new ParameterizedTypeReference<ApiResponse<Void>>() {}
                )
                .block();
    }
}
