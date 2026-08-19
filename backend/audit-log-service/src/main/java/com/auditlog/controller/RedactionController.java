package com.auditlog.controller;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.RedactAuditEventRequest;
import com.auditlog.service.RedactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/events")
public class RedactionController {

    private final RedactionService redactionService;

    public RedactionController(RedactionService redactionService) {
        this.redactionService = redactionService;
    }

    @PostMapping("/{eventId}/redact")
    public ResponseEntity<AuditEventResponse> redactEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody RedactAuditEventRequest request) {
        return ResponseEntity.ok(redactionService.redactEvent(eventId, request));
    }
}
