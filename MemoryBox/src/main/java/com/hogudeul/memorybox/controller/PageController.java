package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.dto.FeedItemView;
import com.hogudeul.memorybox.service.FeedService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    private final FeedService feedService;

    public PageController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/feed")
    public String feedPage(@RequestParam(required = false) String pwdError,
                           @RequestParam(required = false) String pwdChanged,
                           Model model,
                           HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        List<FeedItemView> feedItems = feedService.getImageFeedItems();

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("pwdError", pwdError);
        model.addAttribute("pwdChanged", "true".equals(pwdChanged));
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("authors", feedService.getAuthorFilterOptions(feedItems));
        model.addAttribute("tags", feedService.getTagFilterOptions(feedItems));
        model.addAttribute("years", feedService.getAlbumFilterOptions(feedItems));
        return "feed";
    }


    @GetMapping("/feed/{itemId}")
    public String feedDetailPlaceholder(@PathVariable Long itemId, Model model, HttpSession session) {
        model.addAttribute("itemId", itemId);
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "detail-placeholder";
    }
}
