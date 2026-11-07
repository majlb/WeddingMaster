package com.wedora.repository;

import com.wedora.domain.CollaborationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollaborationItemRepository extends JpaRepository<CollaborationItem, Long> {
    List<CollaborationItem> findByCollaborationIdOrderByCreatedAtAsc(Long collaborationId);
    boolean existsByCollaborationIdAndServiceOfferingId(Long collaborationId, Long serviceOfferingId);
}
