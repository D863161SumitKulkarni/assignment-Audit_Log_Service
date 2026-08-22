package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.CreateAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.repository.AuditEventSpecifications;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

    private static final String GENESIS_HASH = "0".repeat(64);
    private static final long APPEND_LOCK_KEY = 4_155_872_021L;

    private final AuditEventRepository auditEventRepository;
    private final HashService hashService;
    private final JsonUtil jsonUtil;
    private final AuditEventMapper auditEventMapper;
    private final JdbcTemplate jdbcTemplate;

    public AuditEventService(
            AuditEventRepository auditEventRepository,
            HashService hashService,
            JsonUtil jsonUtil,
            AuditEventMapper auditEventMapper,
            JdbcTemplate jdbcTemplate) {
        this.auditEventRepository = auditEventRepository;
        this.hashService = hashService;
        this.jsonUtil = jsonUtil;
        this.auditEventMapper = auditEventMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public AuditEventResponse createEvent(CreateAuditEventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        acquireAppendLock();
        // PostgreSQL TIMESTAMPTZ stores microsecond precision; truncate here so the hashed value
        // matches what is read back, avoiding a false CURRENT_HASH_MISMATCH on verification.
        Instant eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS);
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

    private void acquireAppendLock() {
        // Transaction-scoped PostgreSQL lock serializes appends across application instances.
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(?)",
                preparedStatement -> preparedStatement.setLong(1, APPEND_LOCK_KEY),
                resultSet -> null);
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
        validateTimeRange(from, to);

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

    private void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
