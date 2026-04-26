package com.hogudeul.memorybox.interceptor;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public LoginCheckInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Object loginUserObj = session != null ? session.getAttribute("loginUser") : null;
        LoginUserSession loginUser = null;

        if (loginUserObj instanceof LoginUserSession) {
            loginUser = (LoginUserSession) loginUserObj;
        } else {
            LoginUserSession autoLoginUser = authService.tryAutoLogin(request, response);
            if (autoLoginUser == null) {
                if (session != null) {
                    session.invalidate();
                }
                String originalUrl = request.getRequestURI();
                String query = request.getQueryString();
                if (query != null && !query.isBlank()) {
                    originalUrl += "?" + query;
                }
                String encodedRedirect = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
                response.sendRedirect("/login?expired=true&redirect=" + encodedRedirect);
                return false;
            }

            session = request.getSession(true);
            session.setAttribute("loginUser", autoLoginUser);
            session.setMaxInactiveInterval(60 * 30);
            authService.markSessionAccessUpdatedNow(session);
            loginUser = autoLoginUser;
        }

        authService.updateLastAccessIfDue(session, loginUser.getUserId());
        return true;
    }
}
