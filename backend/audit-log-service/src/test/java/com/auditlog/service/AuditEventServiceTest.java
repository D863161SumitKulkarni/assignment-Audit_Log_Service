package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.CreateAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    private static final String GENESIS_HASH = "0".repeat(64);

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private HashService hashService;

    @Mock
    private JsonUtil jsonUtil;

    @Mock
    private AuditEventMapper auditEventMapper;

    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        auditEventService = new AuditEventService(
                auditEventRepository,
                hashService,
                jsonUtil,
                auditEventMapper);
        when(jsonUtil.toCanonicalJson(any())).thenReturn("{\"key\":\"value\"}");
        when(hashService.calculateAuditEventHash(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                any(Instant.class), anyString())).thenReturn("generated-hash");
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventMapper.toResponse(any(AuditEvent.class)))
                .thenReturn(AuditEventResponse.builder().build());
    }

    @Test
    void firstEventUsesGenesisPreviousHash() {
        when(auditEventRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        auditEventService.createEvent(createRequest());

        verify(hashService).calculateAuditEventHash(
                eq("ACCOUNT_VIEWED"),
                eq("actor-1"),
                eq("CLIENT_ACCOUNT"),
                eq("account-1"),
                eq("{\"key\":\"value\"}"),
                any(Instant.class),
                eq(GENESIS_HASH));
    }

    @Test
    void secondEventUsesLatestCurrentHashAsPreviousHash() {
        AuditEvent latestEvent = AuditEvent.builder()
                .currentHash("latest-current-hash")
                .build();
        when(auditEventRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(latestEvent));

        auditEventService.createEvent(createRequest());

        verify(hashService).calculateAuditEventHash(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                any(Instant.class), eq("latest-current-hash"));
    }

    @Test
    void createEventSavesEventWithGeneratedCurrentHash() {
        when(auditEventRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());
        ArgumentCaptor<AuditEvent> eventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);

        auditEventService.createEvent(createRequest());

        verify(auditEventRepository).save(eventCaptor.capture());
        assertEquals("generated-hash", eventCaptor.getValue().getCurrentHash());
    }

    @Test
    void createEventAssignsServerTimestamp() {
        when(auditEventRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());
        Instant before = Instant.now();

        auditEventService.createEvent(createRequest());

        Instant eventTimestamp = captureSavedEvent().getEventTimestamp();
        Instant after = Instant.now();
        assertNotNull(eventTimestamp);
        assertTrueBetween(eventTimestamp, before, after);
    }

    @Test
    void createEventDoesNotCallUpdateOrDeleteOperations() {
        when(auditEventRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        auditEventService.createEvent(createRequest());

        verify(auditEventRepository, never()).delete(any(AuditEvent.class));
        verify(auditEventRepository, never()).deleteById(any(Long.class));
        verify(auditEventRepository, never()).deleteAll();
        verify(auditEventRepository, never()).deleteAll(any());
    }

    private CreateAuditEventRequest createRequest() {
        return CreateAuditEventRequest.builder()
                .eventType("ACCOUNT_VIEWED")
                .actorId("actor-1")
                .resourceType("CLIENT_ACCOUNT")
                .resourceId("account-1")
                .payload(Map.of("key", "value"))
                .build();
    }

    private AuditEvent captureSavedEvent() {
        ArgumentCaptor<AuditEvent> eventCaptor =
                ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private void assertTrueBetween(Instant value, Instant lower, Instant upper) {
        assertTrue(value.equals(lower) || value.isAfter(lower));
        assertTrue(value.equals(upper) || value.isBefore(upper));
    }
}
