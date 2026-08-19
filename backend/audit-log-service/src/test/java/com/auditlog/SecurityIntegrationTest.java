package com.auditlog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auditlog.audit_log_service.AuditLogServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AuditLogServiceApplication.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void auditApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/audit/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditorCanReadAuditEvents() throws Exception {
        mockMvc.perform(get("/api/audit/events")
                        .with(httpBasic("auditor", "change-me-auditor")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPageSizeIsRejected() throws Exception {
        mockMvc.perform(get("/api/audit/events?size=101")
                        .with(httpBasic("auditor", "change-me-auditor")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedTimestampIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/audit/events?from=not-an-instant")
                        .with(httpBasic("auditor", "change-me-auditor")))
                .andExpect(status().isBadRequest());
    }
}
