package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.util.JsonUtil;
import org.springframework.stereotype.Component;

@Component
public class AuditEventMapper {

    private final JsonUtil jsonUtil;

    public AuditEventMapper(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    public AuditEventResponse toResponse(AuditEvent auditEvent) {
        String payloadJson = auditEvent.isRedacted() && auditEvent.getPayloadRedacted() != null
                ? auditEvent.getPayloadRedacted()
                : auditEvent.getPayloadOriginal();

        return AuditEventResponse.builder()
                .eventId(auditEvent.getEventId())
                .eventType(auditEvent.getEventType())
                .actorId(auditEvent.getActorId())
                .resourceType(auditEvent.getResourceType())
                .resourceId(auditEvent.getResourceId())
                .payload(jsonUtil.fromJsonToMap(payloadJson))
                .eventTimestamp(auditEvent.getEventTimestamp())
                .createdAt(auditEvent.getCreatedAt())
                .previousHash(auditEvent.getPreviousHash())
                .currentHash(auditEvent.getCurrentHash())
                .hashAlgorithm(auditEvent.getHashAlgorithm())
                .archived(auditEvent.isArchived())
                .redacted(auditEvent.isRedacted())
                .build();
    }
}
