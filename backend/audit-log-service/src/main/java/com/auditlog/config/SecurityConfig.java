package com.auditlog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

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
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
                return jwt -> {
                        List<String> roles = jwt.getClaimAsStringList("roles");
                        if (roles == null) {
                                return List.of();
                        }
                        return roles.stream()
                                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                        .map(SimpleGrantedAuthority::new)
                                        .map(authority -> (GrantedAuthority) authority)
                                        .toList();
                };
        }

        private JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
                return converter;
    }
}
