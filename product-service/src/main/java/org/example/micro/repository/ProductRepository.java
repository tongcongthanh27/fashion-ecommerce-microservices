package org.example.micro.repository;

import org.example.micro.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByIdAndIsDeletedFalse(String id);

    List<Product> findAllByIsDeletedFalse();
}
