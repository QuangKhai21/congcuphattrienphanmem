package com.db7.j2ee_quanlythucung.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 thất bại (đổi code lấy token, lấy userinfo, v.v.) mặc định về /login?error nên bị hiểu nhầm là sai mật khẩu form.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 login failed: {} — {}", exception.getClass().getSimpleName(), exception.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("OAuth2 failure detail", exception);
        }
        String ctx = request.getContextPath();
        response.sendRedirect(ctx + "/login?error=oauth2_failed");
    }
}
