package org.example.micro.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.micro.dto.ApiResponse;
import org.example.micro.dto.request.ProductCreateRequest;
import org.example.micro.dto.request.ProductUpdateRequest;
import org.example.micro.dto.response.ProductResponse;
import org.example.micro.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ApiResponse<ProductResponse> response = ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo sản phẩm thành công")
                .result(productService.createProduct(request))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ApiResponse<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "0") int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ApiResponse.<Page<ProductResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm thành công")
                .result(productService.getAllProducts(pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id) {
        return ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin sản phẩm thành công")
                .result(productService.getProductById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật sản phẩm thành công")
                .result(productService.updateProduct(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa sản phẩm thành công")
                .build();
    }
}
