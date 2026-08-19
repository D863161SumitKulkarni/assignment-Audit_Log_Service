package com.auditlog.controller;

import com.auditlog.dto.ComplianceReportResponse;
import com.auditlog.service.ComplianceReportService;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/compliance")
public class ComplianceReportController {

    private final ComplianceReportService complianceReportService;

    public ComplianceReportController(
            ComplianceReportService complianceReportService) {
        this.complianceReportService = complianceReportService;
    }

    @GetMapping("/client-account-access")
    public ResponseEntity<ComplianceReportResponse> getClientAccountAccessReport(
            @RequestParam(required = false) String clientAccountId,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // This endpoint implements the clarified Scenario C prototype scope.
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                complianceReportService.getClientAccountAccessReport(
                        clientAccountId,
                        actorId,
                        from,
                        to,
                        pageable));
    }
}
