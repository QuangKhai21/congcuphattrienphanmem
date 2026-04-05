package com.db7.j2ee_quanlythucung.security;

import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.RoleRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = "GOOGLE";
        String email = oAuth2User.getAttribute("email");
        String name = resolveDisplayName(oAuth2User, email);
        String picture = oAuth2User.getAttribute("picture");

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed: email not available from provider {}", provider);
            response.sendRedirect("/login?error=oauth2_no_email");
            return;
        }

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getProvider() != null && !user.getProvider().equals(provider)) {
                log.warn("User {} tried to login with {} but account uses {}",
                        email, provider, user.getProvider());
                response.sendRedirect("/login?error=oauth2_wrong_provider");
                return;
            }
            user.setFullName(name);
            if (picture != null) user.setAvatarUrl(picture);
            user.setEnabled(true);
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            }
            log.info("OAuth2 login: updated existing user {}", email);
        } else {
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String finalUsername = baseUsername;
            int suffix = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + suffix++;
            }

            Set<Role> roles = new HashSet<>();
            roleRepository.findByName(Role.RoleType.ROLE_CUSTOMER).ifPresent(roles::add);

            user = User.builder()
                    .username(finalUsername)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(name)
                    .email(email)
                    .provider(provider)
                    .providerId(oAuth2User.getAttribute("sub"))
                    .avatarUrl(picture)
                    .enabled(true)
                    .roles(roles)
                    .build();

            user = userRepository.save(user);
            log.info("OAuth2 login: created new user {} via {}", email, provider);
        }

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                new UserDetailsImpl(user),
                null,
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                        .collect(Collectors.toList())
        );
        newAuth.setDetails(authentication.getDetails());

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleType.ROLE_ADMIN);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        response.sendRedirect(isAdmin ? "/admin" : "/dashboard");
    }

    private String resolveDisplayName(OAuth2User oAuth2User, String email) {
        String name = oAuth2User.getAttribute("name");
        if (name != null && !name.isBlank()) return name;
        String givenName = oAuth2User.getAttribute("given_name");
        if (givenName != null && !givenName.isBlank()) return givenName;
        String displayName = oAuth2User.getAttribute("display_name");
        if (displayName != null && !displayName.isBlank()) return displayName;
        return email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    }
}
