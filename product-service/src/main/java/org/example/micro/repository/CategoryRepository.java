package org.example.micro.repository;

import org.example.micro.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {

    Optional<Category> findByIdAndIsDeletedFalse(String id);
}
