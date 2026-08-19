package com.auditlog.audit_log_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication(scanBasePackages = "com.auditlog")
@EntityScan("com.auditlog.entity")
@EnableJpaRepositories("com.auditlog.repository")
public class AuditLogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditLogServiceApplication.class, args);
	}

}
