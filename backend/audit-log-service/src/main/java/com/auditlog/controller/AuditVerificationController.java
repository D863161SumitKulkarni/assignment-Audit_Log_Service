package com.auditlog.controller;

import com.auditlog.dto.VerifyChainResponse;
import com.auditlog.service.ChainVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditVerificationController {

    private final ChainVerificationService chainVerificationService;

    public AuditVerificationController(ChainVerificationService chainVerificationService) {
        this.chainVerificationService = chainVerificationService;
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyChainResponse> verifyChain() {
        return ResponseEntity.ok(chainVerificationService.verifyChain());
    }
}
