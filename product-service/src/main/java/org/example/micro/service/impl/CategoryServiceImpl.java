package org.example.micro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.micro.dto.request.CategoryCreateRequest;
import org.example.micro.dto.request.CategoryUpdateRequest;
import org.example.micro.dto.response.CategoryResponse;
import org.example.micro.entity.Category;
import org.example.micro.exception.AppException;
import org.example.micro.exception.ErrorCode;
import org.example.micro.mapper.CategoryMapper;
import org.example.micro.repository.CategoryRepository;
import org.example.micro.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        Category category = categoryMapper.toEntity(request);
        if (request.getParentId() != null) {
            category.setParentId(findCategory(request.getParentId()));
        }
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAllByIsDeletedFalse(pageable).map(categoryMapper::toResponse);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String id, CategoryUpdateRequest request) {
        Category category = findCategory(id);
        categoryMapper.updateEntity(request, category);
        if (request.getParentId() != null) {
            if (id.equals(request.getParentId())) {
                throw new AppException(ErrorCode.CATEGORY_PARENT_INVALID);
            }
            category.setParentId(findCategory(request.getParentId()));
        }
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(String id) {
        findCategory(id).setIsDeleted(true);
    }

    private Category findCategory(String id) {
        return categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
