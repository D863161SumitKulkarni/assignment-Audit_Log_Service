package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.RedactAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.exception.ResourceNotFoundException;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedactionService {

    private final AuditEventRepository auditEventRepository;
    private final JsonUtil jsonUtil;
    private final AuditEventMapper auditEventMapper;

    public RedactionService(
            AuditEventRepository auditEventRepository,
            JsonUtil jsonUtil,
            AuditEventMapper auditEventMapper) {
        this.auditEventRepository = auditEventRepository;
        this.jsonUtil = jsonUtil;
        this.auditEventMapper = auditEventMapper;
    }

    @Transactional
    public AuditEventResponse redactEvent(
            UUID eventId,
            RedactAuditEventRequest request) {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        AuditEvent auditEvent = auditEventRepository.findByEventId(eventId)
            .orElseThrow(() -> new ResourceNotFoundException(
                        "Audit event not found: " + eventId));

        Map<String, Object> redactedPayload = new HashMap<>(
                jsonUtil.fromJsonToMap(auditEvent.getPayloadOriginal()));
        request.getFieldsToRedact().forEach(redactedPayload::remove);

        Instant redactedAt = Instant.now();
        auditEvent.setPayloadRedacted(jsonUtil.toCanonicalJson(redactedPayload));
        auditEvent.setRedacted(true);
        auditEvent.setRedactedAt(redactedAt);
        auditEvent.setRedactionReason(request.getReason());

        // Redaction changes only the response representation; original payload and hashes remain immutable.
        return auditEventMapper.toResponse(auditEventRepository.save(auditEvent));
    }
}
