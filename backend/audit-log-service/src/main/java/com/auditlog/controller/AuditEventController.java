package com.auditlog.controller;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.CreateAuditEventRequest;
import com.auditlog.service.AuditEventService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping("/events")
    public ResponseEntity<AuditEventResponse> createEvent(
            @Valid @RequestBody CreateAuditEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auditEventService.createEvent(request));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<AuditEventResponse>> queryEvents(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "false") Boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(auditEventService.queryEvents(
                actorId,
                resourceType,
                resourceId,
                eventType,
                from,
                to,
                includeArchived,
                pageable));
    }
}
