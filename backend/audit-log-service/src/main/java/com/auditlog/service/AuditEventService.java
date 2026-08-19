package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.CreateAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.repository.AuditEventSpecifications;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

    private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditEventRepository auditEventRepository;
    private final HashService hashService;
    private final JsonUtil jsonUtil;
    private final AuditEventMapper auditEventMapper;

    public AuditEventService(
            AuditEventRepository auditEventRepository,
            HashService hashService,
            JsonUtil jsonUtil,
            AuditEventMapper auditEventMapper) {
        this.auditEventRepository = auditEventRepository;
        this.hashService = hashService;
        this.jsonUtil = jsonUtil;
        this.auditEventMapper = auditEventMapper;
    }

    @Transactional
    public AuditEventResponse createEvent(CreateAuditEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        Instant eventTimestamp = Instant.now();
        String payloadOriginal = jsonUtil.toCanonicalJson(request.getPayload());
        String previousHash = auditEventRepository.findTopByOrderByIdDesc()
                .map(AuditEvent::getCurrentHash)
                .orElse(GENESIS_HASH);
        String currentHash = hashService.calculateAuditEventHash(
                request.getEventType(),
                request.getActorId(),
                request.getResourceType(),
                request.getResourceId(),
                payloadOriginal,
                eventTimestamp,
                previousHash);

        AuditEvent auditEvent = AuditEvent.builder()
                .eventType(request.getEventType())
                .actorId(request.getActorId())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .payloadOriginal(payloadOriginal)
                .eventTimestamp(eventTimestamp)
                .previousHash(previousHash)
                .currentHash(currentHash)
                .build();

        return auditEventMapper.toResponse(auditEventRepository.save(auditEvent));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> queryEvents(
            String actorId,
            String resourceType,
            String resourceId,
            String eventType,
            Instant from,
            Instant to,
            Boolean includeArchived,
            Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("pageable must not be null");
        }

        Pageable effectivePageable = pageable;
        if (pageable.isPaged() && !pageable.getSort().isSorted()) {
            effectivePageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "id"));
        }

        return auditEventRepository.findAll(
                        AuditEventSpecifications.withFilters(
                                actorId,
                                resourceType,
                                resourceId,
                                eventType,
                                from,
                                to,
                                includeArchived),
                        effectivePageable)
                .map(auditEventMapper::toResponse);
    }
}
