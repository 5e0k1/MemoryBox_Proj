package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginForm;
import com.hogudeul.memorybox.auth.LoginResult;
import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

@Controller
public class AuthController {

    // TODO: Spring Security 미사용 구조이므로 로그아웃/비밀번호 변경 등 상태 변경 요청에 대해 CSRF 보호 적용이 필요합니다.
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(@ModelAttribute("loginForm") LoginForm loginForm,
                            Model model,
                            HttpSession session,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            @RequestParam(required = false) String redirect,
                            @RequestParam(required = false) String kakaoError) {
        String redirectTarget = resolveRedirectTarget(redirect);

        if (session != null && session.getAttribute("loginUser") != null) {
            return "redirect:" + redirectTarget;
        }

        LoginUserSession autoLoginUser = authService.tryAutoLogin(request, response);
        if (autoLoginUser != null) {
            HttpSession loginSession = request.getSession(true);
            loginSession.setAttribute("loginUser", autoLoginUser);
            loginSession.setMaxInactiveInterval(60 * 30);
            authService.markSessionAccessUpdatedNow(loginSession);
            return "redirect:" + redirectTarget;
        }

        if ("true".equals(request.getParameter("expired"))) {
            model.addAttribute("globalError", "세션이 만료되었거나 로그인이 필요합니다.");
        }
        if (kakaoError != null && !kakaoError.isBlank()) {
            model.addAttribute("globalError", kakaoError);
        }
        model.addAttribute("redirect", redirectTarget);
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm loginForm,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model,
                        @RequestParam(required = false) String redirect) {
        String redirectTarget = resolveRedirectTarget(redirect);

        if (bindingResult.hasErrors()) {
            model.addAttribute("globalError", "아이디와 비밀번호를 모두 입력해 주세요.");
            model.addAttribute("redirect", redirectTarget);
            return "login";
        }

        LoginResult result = authService.login(
                loginForm.getLoginId(),
                loginForm.getPassword(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        if (!result.isSuccess()) {
            model.addAttribute("globalError", result.getMessage());
            model.addAttribute("redirect", redirectTarget);
            return "login";
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("loginUser", new LoginUserSession(
                result.getUserAccount().getUserId(),
                result.getUserAccount().getLoginId(),
                result.getUserAccount().getDisplayName(),
                result.getUserAccount().getRole()
        ));
        session.setMaxInactiveInterval(60 * 30);
        authService.markSessionAccessUpdatedNow(session);
        authService.handleLoginSuccess(result.getUserAccount().getUserId(), loginForm.isRememberMe(), response);
        return "redirect:" + redirectTarget;
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        Long userId = null;
        if (session != null) {
            Object loginUser = session.getAttribute("loginUser");
            if (loginUser instanceof LoginUserSession loginUserSession) {
                userId = loginUserSession.getUserId();
            }
        }

        authService.logout(userId, response);

        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    @PostMapping("/account/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String newPasswordConfirm,
                                 HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login?expired=true";
        }

        String errorMessage = authService.changePassword(
                loginUser.getUserId(),
                currentPassword,
                newPassword,
                newPasswordConfirm
        );

        if (errorMessage != null) {
            return "redirect:/feed?pwdError=" + UriUtils.encode(errorMessage, java.nio.charset.StandardCharsets.UTF_8);
        }
        return "redirect:/feed?pwdChanged=true";
    }

    private String resolveRedirectTarget(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return "/feed";
        }
        if (!redirect.startsWith("/") || redirect.startsWith("//")) {
            return "/feed";
        }
        return redirect;
    }
}
