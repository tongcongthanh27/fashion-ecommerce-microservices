package org.example.micro.repository;

import jakarta.persistence.LockModeType;
import org.example.micro.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByIdAndIsDeletedFalse(String id);

    Page<Product> findAllByIsDeletedFalse(Pageable pageable);

    List<Product> findByIdIn(List<String> ids);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findByIdForUpdate(@Param("ids") List<String> ids);

 }
