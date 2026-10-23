package org.example.micro.service;

import org.example.micro.dto.request.ProductRequest;
import org.example.micro.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(String id);
    List<ProductResponse> getAllProducts();
    ProductResponse updateProduct(String id, ProductRequest request);
    void deleteProduct(String id);
}
