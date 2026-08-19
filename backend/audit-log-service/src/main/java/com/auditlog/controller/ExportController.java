package com.auditlog.controller;

import com.auditlog.dto.ExportBundleResponse;
import com.auditlog.service.ExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<ExportBundleResponse> exportByActorId(
            @PathVariable String actorId) {
        return ResponseEntity.ok(exportService.exportByActorId(actorId));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<ExportBundleResponse> exportByResourceId(
            @PathVariable String resourceId) {
        return ResponseEntity.ok(exportService.exportByResourceId(resourceId));
    }
}
