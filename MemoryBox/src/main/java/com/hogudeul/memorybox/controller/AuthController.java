package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginForm;
import com.hogudeul.memorybox.auth.LoginResult;
import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(@ModelAttribute("loginForm") LoginForm loginForm,
                            Model model,
                            HttpSession session,
                            HttpServletRequest request) {
        if (session != null && session.getAttribute("loginUser") != null) {
            return "redirect:/feed";
        }

        if ("true".equals(request.getParameter("expired"))) {
            model.addAttribute("globalError", "세션이 만료되었거나 로그인이 필요합니다.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm loginForm,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("globalError", "아이디와 비밀번호를 모두 입력해 주세요.");
            return "login";
        }

        LoginResult result = authService.login(
                loginForm.getLoginId(),
                loginForm.getPassword(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        if (!result.success()) {
            model.addAttribute("globalError", result.message());
            return "login";
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("loginUser", new LoginUserSession(
                result.userAccount().getUserId(),
                result.userAccount().getLoginId(),
                result.userAccount().getDisplayName(),
                result.userAccount().getRole()
        ));
        session.setMaxInactiveInterval(60 * 30);
        return "redirect:/feed";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
