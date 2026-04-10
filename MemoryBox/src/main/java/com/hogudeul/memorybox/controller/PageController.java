package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.dto.FeedItemView;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @GetMapping("/feed")
    public String feedPage(@RequestParam(required = false) String pwdError,
                           @RequestParam(required = false) String pwdChanged,
                           Model model,
                           HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("pwdError", pwdError);
        model.addAttribute("pwdChanged", "true".equals(pwdChanged));
        model.addAttribute("feedItems", buildMockFeedItems());
        model.addAttribute("authors", List.of("전체", "민수", "지은", "현우", "서연", "지훈", "수빈", "유나"));
        model.addAttribute("tags", List.of("전체", "여행", "일상", "생일", "캠핑", "바다", "반려동물"));
        model.addAttribute("years", List.of("전체", "2026", "2025", "2024", "2023", "2022"));
        return "feed";
    }


    @GetMapping("/feed/{itemId}")
    public String feedDetailPlaceholder(@PathVariable Long itemId, Model model, HttpSession session) {
        model.addAttribute("itemId", itemId);
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "detail-placeholder";
    }

    private List<FeedItemView> buildMockFeedItems() {
        return List.of(
                new FeedItemView(101L, "photo", "https://picsum.photos/id/1018/800/800", "봄 소풍", "민수", 2026, "2026-03-22", 14, 3, new String[]{"여행", "일상"}),
                new FeedItemView(102L, "video", "https://picsum.photos/id/1025/800/800", "강아지 산책 브이로그", "지은", 2025, "2026-03-20", 28, 11, new String[]{"반려동물", "일상"}),
                new FeedItemView(103L, "photo", "https://picsum.photos/id/1035/800/800", "여름 바다", "현우", 2024, "2026-03-18", 6, 0, new String[]{"바다", "여행"}),
                new FeedItemView(104L, "photo", "https://picsum.photos/id/1043/800/800", "캠핑 저녁", "서연", 2025, "2026-03-15", 0, 0, new String[]{"캠핑"}),
                new FeedItemView(105L, "video", "https://picsum.photos/id/1050/800/800", "생일 파티 하이라이트", "지훈", 2023, "2026-03-11", 41, 8, new String[]{"생일", "일상"}),
                new FeedItemView(106L, "photo", "https://picsum.photos/id/1069/800/800", "노을 산책", "수빈", 2022, "2026-03-07", 3, 1, new String[]{"일상"}),
                new FeedItemView(107L, "video", "https://picsum.photos/id/1074/800/800", "여행 스냅 모음", "유나", 2024, "2026-03-01", 18, 5, new String[]{"여행"})
        );
    }
}
