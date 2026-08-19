package com.auditlog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/audit/events")
                        .hasAnyRole("AUDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/audit/events")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/audit/verify",
                                "/api/audit/export/**",
                                "/api/audit/compliance/**")
                        .hasAnyRole("AUDITOR", "ADMIN")
                        .requestMatchers("/api/audit/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                });

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${audit.security.admin.username:admin}") String adminUsername,
            @Value("${audit.security.admin.password:change-me-admin}") String adminPassword,
            @Value("${audit.security.auditor.username:auditor}") String auditorUsername,
            @Value("${audit.security.auditor.password:change-me-auditor}") String auditorPassword) {
        UserDetails admin = User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN", "AUDITOR")
                .build();
        UserDetails auditor = User.withUsername(auditorUsername)
                .password(passwordEncoder.encode(auditorPassword))
                .roles("AUDITOR")
                .build();

        return new InMemoryUserDetailsManager(admin, auditor);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
