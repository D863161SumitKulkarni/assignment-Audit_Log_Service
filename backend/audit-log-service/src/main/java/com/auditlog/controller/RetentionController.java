package com.auditlog.controller;

import com.auditlog.service.RetentionService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/retention")
public class RetentionController {

    private final RetentionService retentionService;

    public RetentionController(RetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @PostMapping("/archive")
    public ResponseEntity<Map<String, Object>> archiveEvents(
            @RequestParam(defaultValue = "90") int days) {
        long archivedCount = retentionService.archiveEventsOlderThan(days);

        return ResponseEntity.ok(Map.of(
                "archivedCount", archivedCount,
                "message", "Audit events archived only; no records were deleted"));
    }
}
