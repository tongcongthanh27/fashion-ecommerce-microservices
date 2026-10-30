package org.example.orderservice.clients;

import org.example.orderservice.dto.client.request.ProductFilter;
import org.example.orderservice.dto.client.request.StockDeductRequest;
import org.example.orderservice.dto.client.response.ProductDTO;

import java.util.List;

public interface ProductClient {
    List<ProductDTO> getProductsByIds(ProductFilter productFilter);
    void updateQuantity(StockDeductRequest request);
}
