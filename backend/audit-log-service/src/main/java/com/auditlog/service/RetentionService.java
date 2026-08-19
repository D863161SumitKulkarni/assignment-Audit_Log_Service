package com.auditlog.service;

import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetentionService {

    private final AuditEventRepository auditEventRepository;

    public RetentionService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public long archiveEventsOlderThan(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative");
        }

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(days * 24L * 60L * 60L);
        Specification<AuditEvent> retentionSpecification = (root, query, criteriaBuilder) -> {
            Predicate olderThanCutoff = criteriaBuilder.lessThan(
                    root.get("eventTimestamp"), cutoff);
            Predicate notArchived = criteriaBuilder.isFalse(root.get("archived"));
            return criteriaBuilder.and(olderThanCutoff, notArchived);
        };

        List<AuditEvent> eventsToArchive = auditEventRepository.findAll(retentionSpecification);
        for (AuditEvent auditEvent : eventsToArchive) {
            auditEvent.setArchived(true);
            auditEvent.setArchivedAt(now);
        }

        // Archival changes metadata only; archived records remain part of chain verification.
        auditEventRepository.saveAll(eventsToArchive);
        return eventsToArchive.size();
    }
}
