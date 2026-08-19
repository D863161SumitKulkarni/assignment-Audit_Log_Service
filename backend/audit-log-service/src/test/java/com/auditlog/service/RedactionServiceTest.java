package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.RedactAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.exception.ResourceNotFoundException;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedactionServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private JsonUtil jsonUtil;

    @Mock
    private AuditEventMapper auditEventMapper;

    private RedactionService redactionService;
    private AuditEvent auditEvent;

    @BeforeEach
    void setUp() {
        redactionService = new RedactionService(
                auditEventRepository,
                jsonUtil,
                auditEventMapper);
        auditEvent = AuditEvent.builder()
                .eventId(EVENT_ID)
                .payloadOriginal("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}")
                .previousHash("previous-hash")
                .currentHash("current-hash")
                .build();

        lenient().when(auditEventRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.of(auditEvent));
        lenient().when(jsonUtil.fromJsonToMap(any(String.class)))
                .thenReturn(Map.of(
                        "name", "Alice",
                        "email", "alice@example.com"));
        lenient().when(jsonUtil.toCanonicalJson(any())).thenReturn("{\"name\":\"Alice\"}");
        lenient().when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditEventMapper.toResponse(any(AuditEvent.class)))
                .thenReturn(AuditEventResponse.builder().build());
    }

    @Test
    void redactsExistingTopLevelPayloadField() {
        redactionService.redactEvent(EVENT_ID, request("email"));

        verify(jsonUtil).toCanonicalJson(Map.of("name", "Alice"));
    }

    @Test
    void ignoresMissingFieldWithoutFailing() {
        redactionService.redactEvent(EVENT_ID, request("missing"));

        verify(auditEventRepository).save(auditEvent);
    }

    @Test
    void setsRedactedTrue() {
        redactionService.redactEvent(EVENT_ID, request("email"));

        assertEquals(true, auditEvent.isRedacted());
    }

    @Test
    void setsRedactedAt() {
        Instant before = Instant.now();

        redactionService.redactEvent(EVENT_ID, request("email"));

        assertNotNull(auditEvent.getRedactedAt());
        assertEquals(true, auditEvent.getRedactedAt().isAfter(before)
                || auditEvent.getRedactedAt().equals(before));
    }

    @Test
    void storesRedactionReason() {
        redactionService.redactEvent(EVENT_ID, request("email"));

        assertEquals("privacy request", auditEvent.getRedactionReason());
    }

    @Test
    void doesNotModifyPayloadOriginal() {
        String originalPayload = auditEvent.getPayloadOriginal();

        redactionService.redactEvent(EVENT_ID, request("email"));

        assertEquals(originalPayload, auditEvent.getPayloadOriginal());
    }

    @Test
    void doesNotModifyHashes() {
        redactionService.redactEvent(EVENT_ID, request("email"));

        assertEquals("current-hash", auditEvent.getCurrentHash());
        assertEquals("previous-hash", auditEvent.getPreviousHash());
    }

        @Test
        void repeatedRedactionDoesNotReExposePreviouslyRemovedField() {
        redactionService.redactEvent(EVENT_ID, request("email"));
        auditEvent.setPayloadRedacted("{\"name\":\"Alice\"}");
        when(jsonUtil.fromJsonToMap(auditEvent.getPayloadRedacted()))
            .thenReturn(Map.of("name", "Alice"));

        redactionService.redactEvent(EVENT_ID, request("phone"));

        verify(jsonUtil, org.mockito.Mockito.times(2))
            .toCanonicalJson(Map.of("name", "Alice"));
        }

        @Test
        void redactsNestedFieldPath() {
        when(jsonUtil.fromJsonToMap(auditEvent.getPayloadOriginal()))
            .thenReturn(Map.of(
                "profile", Map.of("email", "alice@example.com", "name", "Alice")));

        redactionService.redactEvent(EVENT_ID, request("profile.email"));

        verify(jsonUtil).toCanonicalJson(Map.of(
            "profile", Map.of("name", "Alice")));
        }

    @Test
    void throwsResourceNotFoundExceptionWhenEventIdIsMissing() {
        UUID missingEventId = UUID.randomUUID();
        when(auditEventRepository.findByEventId(missingEventId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> redactionService.redactEvent(
                        missingEventId,
                        request("email")));
    }

    private RedactAuditEventRequest request(String field) {
        return RedactAuditEventRequest.builder()
                .fieldsToRedact(java.util.List.of(field))
                .reason("privacy request")
                .build();
    }
}
