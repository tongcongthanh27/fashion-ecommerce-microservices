package org.example.micro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.micro.dto.request.ProductRequest;
import org.example.micro.dto.response.ProductResponse;
import org.example.micro.entity.Category;
import org.example.micro.entity.Product;
import org.example.micro.exception.AppException;
import org.example.micro.exception.ErrorCode;
import org.example.micro.mapper.ProductMapper;
import org.example.micro.repository.CategoryRepository;
import org.example.micro.repository.ProductRepository;
import org.example.micro.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        product.setCategory(findCategory(request.getCategoryId()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllByIsDeletedFalse().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = findProduct(id);
        productMapper.updateEntity(request, product);
        product.setCategory(findCategory(request.getCategoryId()));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        Product product = findProduct(id);
        product.setIsDeleted(true);
    }

    private Product findProduct(String id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findCategory(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
