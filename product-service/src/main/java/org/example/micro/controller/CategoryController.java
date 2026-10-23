package org.example.micro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.micro.dto.ApiResponse;
import org.example.micro.dto.request.CategoryCreateRequest;
import org.example.micro.dto.request.CategoryUpdateRequest;
import org.example.micro.dto.response.CategoryResponse;
import org.example.micro.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.CREATED.value()).message("Tạo danh mục thành công")
                .result(categoryService.createCategory(request)).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ApiResponse<Page<CategoryResponse>> getAllCategories(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "0") int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ApiResponse.<Page<CategoryResponse>>builder().code(HttpStatus.OK.value())
                .message("Lấy danh sách danh mục thành công")
                .result(categoryService.getAllCategories(pageable)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable String id) {
        return ApiResponse.<CategoryResponse>builder().code(HttpStatus.OK.value())
                .message("Lấy thông tin danh mục thành công")
                .result(categoryService.getCategoryById(id)).build();
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.<CategoryResponse>builder().code(HttpStatus.OK.value())
                .message("Cập nhật danh mục thành công")
                .result(categoryService.updateCategory(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<Void>builder().code(HttpStatus.OK.value())
                .message("Xóa danh mục thành công").build();
    }
}
