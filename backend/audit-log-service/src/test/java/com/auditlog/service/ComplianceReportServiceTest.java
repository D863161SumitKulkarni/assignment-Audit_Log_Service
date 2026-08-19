package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auditlog.dto.AuditEventResponse;
import com.auditlog.dto.ComplianceReportResponse;
import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ComplianceReportServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private AuditEventMapper auditEventMapper;

    private ComplianceReportService complianceReportService;

    @BeforeEach
    void setUp() {
        complianceReportService = new ComplianceReportService(
                auditEventRepository,
                auditEventMapper);
    }

    @Test
    void reportsTotalMatchingRecordsNotOnlyCurrentPageSize() {
        AuditEvent event = AuditEvent.builder().build();
        AuditEventResponse response = AuditEventResponse.builder().build();
        PageRequest pageable = PageRequest.of(
                0, 1, Sort.by(Sort.Direction.ASC, "id"));
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(
                        List.of(event),
                        pageable,
                        3));
        when(auditEventMapper.toResponse(event)).thenReturn(response);

        ComplianceReportResponse report = complianceReportService
                .getClientAccountAccessReport(
                        "account-1",
                        "actor-1",
                        null,
                        null,
                        false,
                        pageable);

        assertEquals(3, report.getTotalRecords());
        assertEquals(List.of(response), report.getAccessEvents());
    }

    @Test
    void passesDeterministicAscendingPageableToRepository() {
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id"));
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        complianceReportService.getClientAccountAccessReport(
                null,
                null,
                null,
                null,
                false,
                pageable);

        verify(auditEventRepository).findAll(any(Specification.class),
                org.mockito.ArgumentMatchers.eq(pageable));
    }
}
