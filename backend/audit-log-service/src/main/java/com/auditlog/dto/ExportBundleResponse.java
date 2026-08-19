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
public class ExportBundleResponse {

    private Instant exportedAt;
    private String filterType;
    private String filterValue;
    private List<AuditEventResponse> records;
    private String firstRecordPreviousHash;
    private String lastRecordCurrentHash;
    private String hashAlgorithm;
    private String exportHash;
}
