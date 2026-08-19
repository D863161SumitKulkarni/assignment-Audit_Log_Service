package com.auditlog.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class HashService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String FIELD_DELIMITER = "|";

    public String sha256(String input) {
        requireNonNull(input, "input");

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public String calculateAuditEventHash(
            String eventType,
            String actorId,
            String resourceType,
            String resourceId,
            String payloadCanonicalJson,
            Instant eventTimestamp,
            String previousHash) {
        requireNonNull(eventType, "eventType");
        requireNonNull(actorId, "actorId");
        requireNonNull(resourceType, "resourceType");
        requireNonNull(resourceId, "resourceId");
        requireNonNull(payloadCanonicalJson, "payloadCanonicalJson");
        requireNonNull(eventTimestamp, "eventTimestamp");
        requireNonNull(previousHash, "previousHash");

        String hashInput = String.join(
                FIELD_DELIMITER,
                eventType,
                actorId,
                resourceType,
                resourceId,
                payloadCanonicalJson,
                eventTimestamp.toString(),
                previousHash);

        return sha256(hashInput);
    }

    private void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
