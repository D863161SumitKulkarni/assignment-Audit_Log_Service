package com.auditlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditEventRequest {

    @NotBlank
    private String eventType;

    @NotBlank
    private String actorId;

    @NotBlank
    private String resourceType;

    @NotBlank
    private String resourceId;

    @NotNull
    private Map<String, Object> payload;
}
