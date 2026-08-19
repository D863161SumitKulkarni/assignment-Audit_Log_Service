package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.ExportBundleResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import com.auditlog.util.JsonUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private AuditEventMapper auditEventMapper;

    @Mock
    private HashService hashService;

    @Mock
    private JsonUtil jsonUtil;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(
                auditEventRepository,
                auditEventMapper,
                hashService,
                jsonUtil);
        lenient().when(jsonUtil.toCanonicalJson(any())).thenReturn("canonical-metadata");
        lenient().when(hashService.sha256(anyString())).thenReturn("export-hash");
    }

    @Test
    void includesEveryRecordHashInExportCommitment() {
        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .previousHash("previous")
                .currentHash("current")
                .build();
        when(auditEventRepository.findByActorIdOrderByIdAsc(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(auditEventMapper.toResponse(event))
                .thenReturn(AuditEventResponse.builder().build());

        ExportBundleResponse response = exportService.exportByActorId("actor-1");

        assertEquals("export-hash", response.getExportHash());
        verify(jsonUtil).toCanonicalJson(argThat((Map<String, Object> metadata) ->
                List.of("current").equals(metadata.get("recordHashes"))));
    }

    @Test
    void rejectsExportsLargerThanConfiguredBound() {
        when(auditEventRepository.findByResourceIdOrderByIdAsc(anyString(), any()))
                .thenReturn(new PageImpl<>(
                        List.of(), PageRequest.of(0, 1), 10_001));

        assertThrows(IllegalArgumentException.class,
                () -> exportService.exportByResourceId("resource-1"));
    }
}