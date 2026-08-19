package com.auditlog.service;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.ExportBundleResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {

    private static final String HASH_ALGORITHM = "SHA-256";
        private static final int MAX_EXPORT_RECORDS = 10_000;

    private final AuditEventRepository auditEventRepository;
    private final AuditEventMapper auditEventMapper;
    private final HashService hashService;
    private final JsonUtil jsonUtil;

    public ExportService(
            AuditEventRepository auditEventRepository,
            AuditEventMapper auditEventMapper,
            HashService hashService,
            JsonUtil jsonUtil) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
        this.hashService = hashService;
        this.jsonUtil = jsonUtil;
    }

    @Transactional(readOnly = true)
    public ExportBundleResponse exportByActorId(String actorId) {
        requireFilterValue(actorId, "actorId");
        Page<AuditEvent> page = auditEventRepository.findByActorIdOrderByIdAsc(
                actorId, Pageable.unpaged());
        ensureExportBound(page);
        return buildBundle("actorId", actorId, page.getContent());
    }

    @Transactional(readOnly = true)
    public ExportBundleResponse exportByResourceId(String resourceId) {
        requireFilterValue(resourceId, "resourceId");
        Page<AuditEvent> page = auditEventRepository.findByResourceIdOrderByIdAsc(
                resourceId, Pageable.unpaged());
        ensureExportBound(page);
        return buildBundle("resourceId", resourceId, page.getContent());
    }

    private ExportBundleResponse buildBundle(
            String filterType,
            String filterValue,
            List<AuditEvent> events) {
        Instant exportedAt = Instant.now();
        List<AuditEventResponse> records = events.stream()
                .map(auditEventMapper::toResponse)
                .toList();
        String firstRecordPreviousHash = events.isEmpty()
                ? null
                : events.get(0).getPreviousHash();
        String lastRecordCurrentHash = events.isEmpty()
                ? null
                : events.get(events.size() - 1).getCurrentHash();

        Map<String, Object> hashMetadata = new LinkedHashMap<>();
        hashMetadata.put("filterType", filterType);
        hashMetadata.put("filterValue", filterValue);
        hashMetadata.put("eventIds", events.stream()
                .map(event -> event.getEventId().toString())
                .collect(Collectors.toList()));
        hashMetadata.put("recordHashes", events.stream()
                .map(AuditEvent::getCurrentHash)
                .collect(Collectors.toList()));
        hashMetadata.put("firstRecordPreviousHash", firstRecordPreviousHash);
        hashMetadata.put("lastRecordCurrentHash", lastRecordCurrentHash);

        String exportHash = hashService.sha256(jsonUtil.toCanonicalJson(hashMetadata));

        return ExportBundleResponse.builder()
                .exportedAt(exportedAt)
                .filterType(filterType)
                .filterValue(filterValue)
                .records(records)
                .firstRecordPreviousHash(firstRecordPreviousHash)
                .lastRecordCurrentHash(lastRecordCurrentHash)
                .hashAlgorithm(HASH_ALGORITHM)
                .exportHash(exportHash)
                .build();
    }

    private void requireFilterValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

        private void ensureExportBound(Page<AuditEvent> page) {
                if (page.getTotalElements() > MAX_EXPORT_RECORDS) {
                        throw new IllegalArgumentException(
                                        "Export exceeds the maximum of " + MAX_EXPORT_RECORDS + " records");
                }
        }
}
