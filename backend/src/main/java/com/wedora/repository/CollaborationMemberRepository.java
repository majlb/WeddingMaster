package com.wedora.repository;

import com.wedora.domain.CollaborationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaborationMemberRepository extends JpaRepository<CollaborationMember, Long> {
    List<CollaborationMember> findByCollaborationIdOrderByCreatedAtAsc(Long collaborationId);
    Optional<CollaborationMember> findByCollaborationIdAndSupplierId(Long collaborationId, Long supplierId);
    boolean existsByCollaborationIdAndSupplierId(Long collaborationId, Long supplierId);
}
