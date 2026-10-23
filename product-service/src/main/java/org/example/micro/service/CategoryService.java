package org.example.micro.service;

import org.example.micro.dto.request.CategoryCreateRequest;
import org.example.micro.dto.request.CategoryUpdateRequest;
import org.example.micro.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    CategoryResponse createCategory(CategoryCreateRequest request);
    CategoryResponse getCategoryById(String id);
    Page<CategoryResponse> getAllCategories(Pageable pageable);
    CategoryResponse updateCategory(String id, CategoryUpdateRequest request);
    void deleteCategory(String id);
}
