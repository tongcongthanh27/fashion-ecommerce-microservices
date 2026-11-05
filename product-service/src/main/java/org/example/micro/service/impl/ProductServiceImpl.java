package org.example.micro.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.micro.dto.client.request.StockDeductRequest;
import org.example.micro.dto.client.request.StockItemRequest;
import org.example.micro.dto.request.ProductCreateRequest;
import org.example.micro.dto.client.request.ProductFilter;
import org.example.micro.dto.request.ProductUpdateRequest;
import org.example.micro.dto.response.ProductResponse;
import org.example.micro.entity.Category;
import org.example.micro.entity.Product;
import org.example.micro.events.ProductLockedEvent;
import org.example.micro.exception.AppException;
import org.example.micro.exception.ErrorCode;
import org.example.micro.mapper.ProductMapper;
import org.example.micro.repository.CategoryRepository;
import org.example.micro.repository.ProductRepository;
import org.example.micro.service.ProductService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
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
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAllByIsDeletedFalse(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        Product product = findProduct(id);
        productMapper.updateEntity(request, product);
        if (request.getCategoryId() != null) {
            product.setCategory(findCategory(request.getCategoryId()));
        }
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        Product product = findProduct(id);
        product.setIsDeleted(true);
    }

    @Override
    public List<ProductResponse> search(ProductFilter productFilter) {
        return productMapper.toResponseList(productRepository.findByIdIn(productFilter.getIds()));
    }

    @Override
    @Transactional
    public void updateStock(StockDeductRequest request) {
        List<StockItemRequest> stockItemRequests = request.getStockItemRequests();
        List<String> sortedIds = request.getStockItemRequests().stream()
                .map(StockItemRequest::getProductId)
                .sorted()
                .collect(Collectors.toList());

        String lockKey = "lock:products:" + String.join(",", sortedIds);
        RLock lock = redissonClient.getLock(lockKey);

        try{
            if (lock.tryLock(10,5, TimeUnit.SECONDS)){
                Thread.sleep(4000);
                log.info("Acquired Redis lock for: {}", lockKey);
                Map<String, Integer> productStockMap = new HashMap<>();
                for (StockItemRequest item : request.getStockItemRequests()) {
                    productStockMap.put(item.getProductId(), item.getQuantity());
                }
                List<Product> products = productRepository.findByIdIn(sortedIds);
                for (Product product: products){
                    Integer quantity = productStockMap.get(product.getId());
                    product.setStock(product.getStock() - quantity);
                }
                ProductLockedEvent productLockedEvent = new ProductLockedEvent();
                productLockedEvent.setOrderId(request.getOrderId());
                kafkaTemplate.send("lock_product", productLockedEvent);
                log.info("Da gui message len kafka {}", productLockedEvent);
            }
            else {
                throw new RuntimeException("Server busy, please try again later");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Proccess interrupted");
        }
        finally {
            log.info("Waiting for unlock [{}]", lockKey);
            if (lock.isHeldByCurrentThread()){
                lock.unlock();
                log.info("Unlock success for [{}]", lockKey);
            }
        }
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
