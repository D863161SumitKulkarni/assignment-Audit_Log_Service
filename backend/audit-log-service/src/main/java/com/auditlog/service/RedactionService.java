package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.RedactAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.exception.ResourceNotFoundException;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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

        String sourcePayload = auditEvent.getPayloadRedacted() != null
            ? auditEvent.getPayloadRedacted()
            : auditEvent.getPayloadOriginal();
        Map<String, Object> redactedPayload = copyMap(
            jsonUtil.fromJsonToMap(sourcePayload));
        request.getFieldsToRedact().forEach(field -> removeField(redactedPayload, field));

        Instant redactedAt = Instant.now();
        auditEvent.setPayloadRedacted(jsonUtil.toCanonicalJson(redactedPayload));
        auditEvent.setRedacted(true);
        auditEvent.setRedactedAt(redactedAt);
        auditEvent.setRedactionReason(request.getReason());

        // Redaction changes only the response representation; original payload and hashes remain immutable.
        return auditEventMapper.toResponse(auditEventRepository.save(auditEvent));
    }

    @SuppressWarnings("unchecked")
    private void removeField(Map<String, Object> payload, String fieldPath) {
        String[] path = fieldPath.split("\\.");
        Map<String, Object> current = payload;
        for (int index = 0; index < path.length - 1; index++) {
            Object nested = current.get(path[index]);
            if (!(nested instanceof Map<?, ?> nestedMap)) {
                return;
            }
            current = (Map<String, Object>) nestedMap;
        }
        current.remove(path[path.length - 1]);
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, copyValue(value)));
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap((Map<String, Object>) map);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::copyValue).toList();
        }
        return value;
    }
}
