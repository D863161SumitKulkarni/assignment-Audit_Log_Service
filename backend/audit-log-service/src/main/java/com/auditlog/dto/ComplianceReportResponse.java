package com.auditlog.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportResponse {

    private Instant generatedAt;
    private String clientAccountId;
    private long totalRecords;
    private List<AuditEventResponse> accessEvents;
}
