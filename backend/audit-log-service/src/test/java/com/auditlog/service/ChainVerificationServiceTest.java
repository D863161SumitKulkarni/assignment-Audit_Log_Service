package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.auditlog.dto.VerifyChainResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChainVerificationServiceTest {

    private static final String GENESIS_HASH = "0".repeat(64);
    private static final Instant EVENT_TIMESTAMP =
            Instant.parse("2026-08-19T10:15:30Z");

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private HashService hashService;

        @Mock
        private JsonUtil jsonUtil;

    private ChainVerificationService chainVerificationService;

    @BeforeEach
    void setUp() {
        chainVerificationService = new ChainVerificationService(
                auditEventRepository,
                hashService,
                jsonUtil);
        lenient().when(jsonUtil.fromJsonToMap(any()))
                .thenReturn(java.util.Map.of("value", "original"));
        lenient().when(jsonUtil.toCanonicalJson(any()))
                .thenReturn("original-payload");
    }

    @Test
    void emptyChainReturnsIntact() {
        when(auditEventRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of());

        VerifyChainResponse response = chainVerificationService.verifyChain();

        assertTrue(response.isChainIntact());
        assertEquals(0, response.getCheckedRecords());
    }

    @Test
    void validChainWithTwoRecordsReturnsIntact() {
        AuditEvent first = event(1L, GENESIS_HASH, "hash-1", false, "original-1");
        AuditEvent second = event(2L, "hash-1", "hash-2", false, "original-2");
        when(auditEventRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(first, second));
        when(hashService.calculateAuditEventHash(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("hash-1", "hash-2");

        VerifyChainResponse response = chainVerificationService.verifyChain();

        assertTrue(response.isChainIntact());
        assertEquals(2, response.getCheckedRecords());
    }

    @Test
    void previousHashMismatchReturnsFailure() {
        AuditEvent event = event(1L, "incorrect-previous", "hash-1", false, "original");
        when(auditEventRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(event));

        VerifyChainResponse response = chainVerificationService.verifyChain();

        assertFalse(response.isChainIntact());
        assertEquals("PREVIOUS_HASH_MISMATCH", response.getViolationType());
    }

    @Test
    void currentHashMismatchReturnsFailure() {
        AuditEvent event = event(1L, GENESIS_HASH, "stored-hash", false, "original");
        when(auditEventRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(event));
        when(hashService.calculateAuditEventHash(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("recalculated-hash");

        VerifyChainResponse response = chainVerificationService.verifyChain();

        assertFalse(response.isChainIntact());
        assertEquals("CURRENT_HASH_MISMATCH", response.getViolationType());
    }

    @Test
    void archivedRecordIsIncludedInVerification() {
        AuditEvent archivedEvent = event(1L, GENESIS_HASH, "hash-1", true, "original");
        when(auditEventRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(archivedEvent));
        when(hashService.calculateAuditEventHash(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("hash-1");

        VerifyChainResponse response = chainVerificationService.verifyChain();

        assertTrue(response.isChainIntact());
        assertEquals(1, response.getCheckedRecords());
    }

    @Test
    void redactedRecordVerifiesUsingOriginalPayload() {
        AuditEvent redactedEvent = event(1L, GENESIS_HASH, "hash-1", false, "original-payload");
        redactedEvent.setRedacted(true);
        redactedEvent.setPayloadRedacted("redacted-payload");
        when(auditEventRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(redactedEvent));
        when(hashService.calculateAuditEventHash(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("hash-1");

        VerifyChainResponse response = chainVerificationService.verifyChain();

        assertTrue(response.isChainIntact());
        verify(hashService).calculateAuditEventHash(
                eq("ACCOUNT_VIEWED"),
                eq("actor-1"),
                eq("CLIENT_ACCOUNT"),
                eq("account-1"),
                eq("original-payload"),
                eq(EVENT_TIMESTAMP),
                eq(GENESIS_HASH));
    }

    private AuditEvent event(
            Long id,
            String previousHash,
            String currentHash,
            boolean archived,
            String payloadOriginal) {
        return AuditEvent.builder()
                .id(id)
                .eventType("ACCOUNT_VIEWED")
                .actorId("actor-1")
                .resourceType("CLIENT_ACCOUNT")
                .resourceId("account-1")
                .payloadOriginal(payloadOriginal)
                .eventTimestamp(EVENT_TIMESTAMP)
                .previousHash(previousHash)
                .currentHash(currentHash)
                .archived(archived)
                .build();
    }
}
