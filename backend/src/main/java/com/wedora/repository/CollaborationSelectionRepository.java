package com.wedora.repository;

import com.wedora.domain.CollaborationSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaborationSelectionRepository extends JpaRepository<CollaborationSelection, Long> {
    List<CollaborationSelection> findByCustomerId(Long customerId);
    Optional<CollaborationSelection> findByCollaborationIdAndCustomerId(Long collaborationId, Long customerId);
    boolean existsByCollaborationIdAndCustomerId(Long collaborationId, Long customerId);
    void deleteByCollaborationIdAndCustomerId(Long collaborationId, Long customerId);
}
