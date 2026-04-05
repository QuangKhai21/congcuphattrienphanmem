package com.db7.j2ee_quanlythucung.config;

import com.db7.j2ee_quanlythucung.security.OAuth2LoginFailureHandler;
import com.db7.j2ee_quanlythucung.security.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.google.client-id",
    havingValue = "", matchIfMissing = false)
public class OAuth2LoginSecurityConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        // Double-check: only activate when client-id is truly set
        if (clientId == null || clientId.isBlank() || "disabled".equalsIgnoreCase(clientId)) {
            return http.build(); // return empty chain, OAuth2 disabled
        }

        http
            .securityMatcher("/login/oauth2/**", "/oauth2/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .failureHandler(oAuth2LoginFailureHandler)
                .successHandler(oAuth2LoginSuccessHandler)
                .permitAll()
            );
        return http.build();
    }
}