package org.example.micro.repository;

import org.example.micro.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByIdAndIsDeletedFalse(String id);

    Page<Product> findAllByIsDeletedFalse(Pageable pageable);

    List<Product> findByIdIn(List<String> ids);
 }
