package com.db7.j2ee_quanlythucung.config;

import com.db7.j2ee_quanlythucung.security.CustomUserDetailsService;
import com.db7.j2ee_quanlythucung.security.OAuth2LoginFailureHandler;
import com.db7.j2ee_quanlythucung.security.OAuth2LoginSuccessHandler;
import com.db7.j2ee_quanlythucung.security.RoleBasedAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final PasswordEncoder passwordEncoder;
    private final RoleBasedAuthenticationSuccessHandler roleBasedAuthSuccessHandler;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Spring Security 6 mặc định dùng XorCsrfTokenRequestAttributeHandler — chỉ gửi hidden _csrf
        // (form Thymeleaf) dễ bị 403. Dùng handler thường để khớp với form server-rendered.
        // Lưu CSRF trong session (không dùng cookie) — tránh lệch token cookie/form lần POST đầu (403).
        HttpSessionCsrfTokenRepository tokenRepository = new HttpSessionCsrfTokenRepository();
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        http
            .authorizeHttpRequests(auth -> auth
                // /logout：匿名访问时不能再要求 authenticated，否则登出后或 GET /logout 会 403
                .requestMatchers("/", "/login", "/logout", "/register", "/forgot-password", "/reset-password",
                    "/oauth2/**", "/login/oauth2/**",
                    "/css/**", "/js/**", "/images/**", "/img/**", "/uploads/**", "/h2-console/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                .requestMatchers("/products/admin/**").hasAnyRole("ADMIN")
                .requestMatchers("/products").permitAll()
                .requestMatchers("/products/{id:\\d+}").permitAll()
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF", "VET")
                .requestMatchers("/lost-pets").permitAll()
                .requestMatchers("/lost-pets/map").permitAll()
                .requestMatchers("/lost-pets/report/{id}").permitAll()
                .requestMatchers("/lost-pets/search").permitAll()
                .requestMatchers("/lost-pets/nearby").permitAll()
                .requestMatchers("/lost-pets/api/**").permitAll()
                .requestMatchers("/services").permitAll()
                .requestMatchers("/services/nearby").permitAll()
                .requestMatchers("/services/24h").permitAll()
                .requestMatchers("/services/pet-taxi").permitAll()
                .requestMatchers("/services/detail/{id}").permitAll()
                .requestMatchers("/services/api/**").permitAll()
                .requestMatchers("/vet-qa").permitAll()
                .requestMatchers("/vet-qa/register").authenticated()
                .requestMatchers("/vet-qa/edit").authenticated()
                .requestMatchers("/vet-qa/unregister").authenticated()
                .requestMatchers("/vet-qa/faq").permitAll()
                .requestMatchers("/vet-qa/question/{id}").permitAll()
                .requestMatchers("/vet-qa/ask").permitAll()
                .requestMatchers("/vet-qa/my-questions").authenticated()
                .requestMatchers("/vet-qa/question/*/answer").hasAnyRole("ADMIN", "VET")
                .requestMatchers("/vet/{id}").permitAll()
                .requestMatchers("/api/conversations/vet/{vetId}").authenticated()
                .requestMatchers("/api/conversations/private/{userId}").authenticated()
                .requestMatchers("/api/users/search").authenticated()
                .requestMatchers("/api/conversations/{id}/messages").authenticated()
                .requestMatchers("/api/conversations/{id}/read").authenticated()
                .requestMatchers("/api/conversations").authenticated()
                .requestMatchers("/health/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleBasedAuthSuccessHandler)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .failureHandler(oAuth2LoginFailureHandler)
                .successHandler(oAuth2LoginSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(tokenRepository)
                .csrfTokenRequestHandler(requestHandler)
                // 登出 POST 若与 Cookie CSRF 不同步易 403；忽略 /logout 的 CSRF（常见取舍）
                .ignoringRequestMatchers("/h2-console/**", "/logout")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
