package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class HashServiceTest {

    private final HashService hashService = new HashService();

    @Test
    void sha256Returns64CharacterLowercaseHex() {
        String hash = hashService.sha256("audit-event");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void sameInputProducesSameHash() {
        assertEquals(
                hashService.sha256("same-input"),
                hashService.sha256("same-input"));
    }

    @Test
    void differentInputProducesDifferentHash() {
        assertNotEquals(
                hashService.sha256("input-one"),
                hashService.sha256("input-two"));
    }

    @Test
    void calculateAuditEventHashIncludesPreviousHash() {
        Instant timestamp = Instant.parse("2026-08-19T10:15:30Z");

        String firstHash = hashService.calculateAuditEventHash(
                "ACCOUNT_VIEWED",
                "actor-1",
                "CLIENT_ACCOUNT",
                "account-1",
                "{\"status\":\"ok\"}",
                timestamp,
                "0".repeat(64));
        String secondHash = hashService.calculateAuditEventHash(
                "ACCOUNT_VIEWED",
                "actor-1",
                "CLIENT_ACCOUNT",
                "account-1",
                "{\"status\":\"ok\"}",
                timestamp,
                "1".repeat(64));

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void sha256RejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> hashService.sha256(null));
    }
}
