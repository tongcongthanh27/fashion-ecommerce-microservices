package org.example.micro.repository;

import org.example.micro.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {

    Optional<Category> findByIdAndIsDeletedFalse(String id);

    Page<Category> findAllByIsDeletedFalse(Pageable pageable);
}
