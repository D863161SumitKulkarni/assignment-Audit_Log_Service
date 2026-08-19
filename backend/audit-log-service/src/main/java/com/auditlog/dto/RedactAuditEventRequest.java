package com.auditlog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedactAuditEventRequest {

    @NotEmpty
    private List<String> fieldsToRedact;

    @NotBlank
    private String reason;
}
