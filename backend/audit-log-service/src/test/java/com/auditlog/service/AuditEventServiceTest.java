package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.CreateAuditEventRequest;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

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

        @Mock
        private JdbcTemplate jdbcTemplate;

    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        auditEventService = new AuditEventService(
                auditEventRepository,
                hashService,
                jsonUtil,
                auditEventMapper,
                jdbcTemplate);
        lenient().when(jsonUtil.toCanonicalJson(any())).thenReturn("{\"key\":\"value\"}");
        lenient().when(hashService.calculateAuditEventHash(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                any(Instant.class), anyString())).thenReturn("generated-hash");
        lenient().when(auditEventRepository.save(any(AuditEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditEventMapper.toResponse(any(AuditEvent.class)))
                .thenReturn(AuditEventResponse.builder().build());
    }

    @Test
    void firstEventUsesGenesisPreviousHash() {
        when(auditEventRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        auditEventService.createEvent(createRequest());

        verifyAppendLockAcquired();
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

        verifyAppendLockAcquired();
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
        // The service truncates to microseconds, so compare against an equally truncated lower bound.
        assertTrueBetween(eventTimestamp, before.truncatedTo(ChronoUnit.MICROS), after);
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

        @Test
        void queryRejectsReversedTimeRange() {
                assertThrows(IllegalArgumentException.class, () -> auditEventService.queryEvents(
                                null,
                                null,
                                null,
                                null,
                                Instant.parse("2026-08-20T00:00:00Z"),
                                Instant.parse("2026-08-19T00:00:00Z"),
                                false,
                                org.springframework.data.domain.PageRequest.of(0, 20)));
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

        private void verifyAppendLockAcquired() {
                verify(jdbcTemplate).query(
                                eq("SELECT pg_advisory_xact_lock(?)"),
                                any(PreparedStatementSetter.class),
                                ArgumentMatchers.<ResultSetExtractor<Void>>any());
        }

    private void assertTrueBetween(Instant value, Instant lower, Instant upper) {
        assertTrue(value.equals(lower) || value.isAfter(lower));
        assertTrue(value.equals(upper) || value.isBefore(upper));
    }
}
