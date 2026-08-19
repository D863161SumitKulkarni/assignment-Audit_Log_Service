package com.auditlog.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyChainResponse {

    private boolean chainIntact;
    private long checkedRecords;
    private UUID firstBrokenEventId;
    private Long firstBrokenDatabaseId;
    private String violationType;
    private String expectedValue;
    private String actualValue;
    private String message;
}
