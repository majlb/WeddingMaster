package com.wedora.repository;

import com.wedora.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlugAndActiveTrue(String slug);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
