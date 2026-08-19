package com.auditlog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.security")
public class SecurityProperties {

    private Credentials admin = new Credentials();
    private Credentials auditor = new Credentials();

    public Credentials getAdmin() {
        return admin;
    }

    public void setAdmin(Credentials admin) {
        this.admin = admin;
    }

    public Credentials getAuditor() {
        return auditor;
    }

    public void setAuditor(Credentials auditor) {
        this.auditor = auditor;
    }

    public static class Credentials {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
