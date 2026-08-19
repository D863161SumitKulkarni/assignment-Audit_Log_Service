package com.auditlog.repository;

import com.auditlog.entity.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    Optional<AuditEvent> findTopByOrderByIdDesc();

    Optional<AuditEvent> findByEventId(UUID eventId);

    List<AuditEvent> findAllByOrderByIdAsc();

    Page<AuditEvent> findByActorIdOrderByIdAsc(String actorId, Pageable pageable);

    Page<AuditEvent> findByResourceIdOrderByIdAsc(String resourceId, Pageable pageable);
}
