package com.hogudeul.memorybox.interceptor;

import com.hogudeul.memorybox.auth.LoginUserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loginUser") == null) {
            response.sendRedirect("/login?expired=true");
            return false;
        }

        Object loginUser = session.getAttribute("loginUser");
        if (!(loginUser instanceof LoginUserSession)) {
            session.invalidate();
            response.sendRedirect("/login?expired=true");
            return false;
        }
        return true;
    }
}
