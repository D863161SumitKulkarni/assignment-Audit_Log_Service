package com.auditlog.service;

import com.auditlog.dto.VerifyChainResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChainVerificationService {

    private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditEventRepository auditEventRepository;
    private final HashService hashService;

    public ChainVerificationService(
            AuditEventRepository auditEventRepository,
            HashService hashService) {
        this.auditEventRepository = auditEventRepository;
        this.hashService = hashService;
    }

    public VerifyChainResponse verifyChain() {
        List<AuditEvent> auditEvents = auditEventRepository.findAllByOrderByIdAsc();
        String expectedPreviousHash = GENESIS_HASH;
        long checkedRecords = 0;

        for (AuditEvent auditEvent : auditEvents) {
            checkedRecords++;

            if (!expectedPreviousHash.equals(auditEvent.getPreviousHash())) {
                return brokenChainResponse(
                        checkedRecords,
                        auditEvent,
                        "PREVIOUS_HASH_MISMATCH",
                        expectedPreviousHash,
                        auditEvent.getPreviousHash(),
                        "Previous hash does not match the expected chain value");
            }

            String recalculatedHash = hashService.calculateAuditEventHash(
                    auditEvent.getEventType(),
                    auditEvent.getActorId(),
                    auditEvent.getResourceType(),
                    auditEvent.getResourceId(),
                    auditEvent.getPayloadOriginal(),
                    auditEvent.getEventTimestamp(),
                    auditEvent.getPreviousHash());

            if (!recalculatedHash.equals(auditEvent.getCurrentHash())) {
                return brokenChainResponse(
                        checkedRecords,
                        auditEvent,
                        "CURRENT_HASH_MISMATCH",
                        recalculatedHash,
                        auditEvent.getCurrentHash(),
                        "Current hash does not match the recalculated event hash");
            }

            expectedPreviousHash = auditEvent.getCurrentHash();
        }

        return VerifyChainResponse.builder()
                .chainIntact(true)
                .checkedRecords(checkedRecords)
                .message("Audit event hash chain is intact")
                .build();
    }

    private VerifyChainResponse brokenChainResponse(
            long checkedRecords,
            AuditEvent auditEvent,
            String violationType,
            String expectedValue,
            String actualValue,
            String message) {
        return VerifyChainResponse.builder()
                .chainIntact(false)
                .checkedRecords(checkedRecords)
                .firstBrokenEventId(auditEvent.getEventId())
                .firstBrokenDatabaseId(auditEvent.getId())
                .violationType(violationType)
                .expectedValue(expectedValue)
                .actualValue(actualValue)
                .message(message)
                .build();
    }
}
