package org.example.micro.service;

import org.example.micro.dto.request.ProductCreateRequest;
import org.example.micro.dto.request.ProductUpdateRequest;
import org.example.micro.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse getProductById(String id);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    ProductResponse updateProduct(String id, ProductUpdateRequest request);
    void deleteProduct(String id);
}
