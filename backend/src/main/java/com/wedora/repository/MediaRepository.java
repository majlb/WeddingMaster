package com.wedora.repository;

import com.wedora.domain.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
}
