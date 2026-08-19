package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.ComplianceReportResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceReportService {

    private static final String CLIENT_ACCOUNT_RESOURCE_TYPE = "CLIENT_ACCOUNT";
    private static final List<String> ACCESS_EVENT_TYPES = List.of(
            "ACCOUNT_VIEWED",
            "ACCOUNT_EXPORTED",
            "ACCOUNT_UPDATED",
            "PERMISSION_GRANTED");

    private final AuditEventRepository auditEventRepository;
    private final AuditEventMapper auditEventMapper;

    public ComplianceReportService(
            AuditEventRepository auditEventRepository,
            AuditEventMapper auditEventMapper) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
    }

    @Transactional(readOnly = true)
    public ComplianceReportResponse getClientAccountAccessReport(
            String clientAccountId,
            String actorId,
            Instant from,
            Instant to,
            Boolean includeArchived,
            Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("pageable must not be null");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        Specification<AuditEvent> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(
                    root.get("resourceType"), CLIENT_ACCOUNT_RESOURCE_TYPE));
            predicates.add(root.get("eventType").in(ACCESS_EVENT_TYPES));
            if (!Boolean.TRUE.equals(includeArchived)) {
                predicates.add(criteriaBuilder.isFalse(root.get("archived")));
            }

            if (clientAccountId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("resourceId"), clientAccountId));
            }
            if (actorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorId"), actorId));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("eventTimestamp"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("eventTimestamp"), to));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditEvent> resultPage = auditEventRepository.findAll(specification, pageable);
        List<AuditEventResponse> accessEvents = resultPage.getContent().stream()
                .map(auditEventMapper::toResponse)
                .toList();

        return ComplianceReportResponse.builder()
                .generatedAt(Instant.now())
                .clientAccountId(clientAccountId)
                .totalRecords(resultPage.getTotalElements())
                .accessEvents(accessEvents)
                .build();
    }
}
