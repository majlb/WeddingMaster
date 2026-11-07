package com.wedora.repository;

import com.wedora.domain.SupplierCollaboration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierCollaborationRepository extends JpaRepository<SupplierCollaboration, Long> {
    java.util.Optional<SupplierCollaboration> findByIdAndActiveTrue(Long id);

    @Query("""
            select distinct c from SupplierCollaboration c
            join CollaborationMember m on m.collaboration = c
            where m.supplier.id = :supplierId
            order by c.createdAt desc
            """)
    List<SupplierCollaboration> findForSupplier(@Param("supplierId") Long supplierId);

    @Query("""
            select distinct c from SupplierCollaboration c
            join CollaborationMember m on m.collaboration = c
            where c.active = true
              and m.status = com.wedora.domain.CollaborationMemberStatus.ACCEPTED
              and (select count(m2) from CollaborationMember m2
                   where m2.collaboration = c
                     and m2.status = com.wedora.domain.CollaborationMemberStatus.ACCEPTED) >= 2
            order by c.createdAt desc
            """)
    List<SupplierCollaboration> findPublicCollaborations();
}
