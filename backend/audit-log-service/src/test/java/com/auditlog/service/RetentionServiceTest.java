package com.auditlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auditlog.entity.AuditEvent;
import com.auditlog.repository.AuditEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private RetentionService retentionService;

    @BeforeEach
    void setUp() {
        retentionService = new RetentionService(auditEventRepository);
    }

    @Test
    void archivesMatchingEventsWithoutChangingIntegrityFields() {
        AuditEvent event = AuditEvent.builder()
                .eventTimestamp(Instant.now().minusSeconds(91L * 24 * 60 * 60))
                .previousHash("previous")
                .currentHash("current")
                .payloadOriginal("original")
                .archived(false)
                .build();
        when(auditEventRepository.findAll(
            ArgumentMatchers.<Specification<AuditEvent>>any()))
                .thenReturn(List.of(event));

        long archivedCount = retentionService.archiveEventsOlderThan(90);

        assertEquals(1, archivedCount);
        assertEquals(true, event.isArchived());
        assertEquals("previous", event.getPreviousHash());
        assertEquals("current", event.getCurrentHash());
        assertEquals("original", event.getPayloadOriginal());
        verify(auditEventRepository).saveAll(List.of(event));
    }

    @Test
    void returnsZeroWhenNoEventsMatch() {
        when(auditEventRepository.findAll(
            ArgumentMatchers.<Specification<AuditEvent>>any()))
                .thenReturn(List.of());

        assertEquals(0, retentionService.archiveEventsOlderThan(90));
    }

    @Test
    void rejectsNegativeRetentionWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> retentionService.archiveEventsOlderThan(-1));
    }
}
