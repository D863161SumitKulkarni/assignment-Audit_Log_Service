package com.auditlog.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse {

    private UUID eventId;
    private String eventType;
    private String actorId;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> payload;
    private Instant eventTimestamp;
    private Instant createdAt;
    private String previousHash;
    private String currentHash;
    private String hashAlgorithm;
    private boolean archived;
    private boolean redacted;
}
