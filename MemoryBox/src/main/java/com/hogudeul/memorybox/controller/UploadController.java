package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.UploadService;
import com.hogudeul.memorybox.upload.MultiPhotoUploadForm;
import com.hogudeul.memorybox.upload.SinglePhotoUploadForm;
import com.hogudeul.memorybox.upload.UploadException;
import com.hogudeul.memorybox.upload.VideoUploadForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @GetMapping("/upload")
    public String uploadMenu(HttpSession session, Model model) {
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "upload/menu";
    }

    @GetMapping("/upload/photo")
    public String singlePhotoPage(HttpSession session, Model model) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("albums", uploadService.getActiveAlbums(loginUser.getUserId()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new SinglePhotoUploadForm());
        }
        return "upload/single-photo";
    }

    @PostMapping("/upload/photo")
    public String uploadSinglePhoto(@ModelAttribute("form") SinglePhotoUploadForm form,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        try {
            uploadService.uploadSinglePhoto(loginUser.getUserId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "사진 1장 업로드가 완료되었습니다.");
            return "redirect:/feed";
        } catch (UploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/upload/photo";
        }
    }

    @GetMapping("/upload/photos")
    public String multiPhotoPage(HttpSession session, Model model) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("albums", uploadService.getActiveAlbums(loginUser.getUserId()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new MultiPhotoUploadForm());
        }
        return "upload/multi-photo";
    }

    @PostMapping("/upload/photos")
    public String uploadMultiPhoto(@ModelAttribute("form") MultiPhotoUploadForm form,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        try {
            uploadService.uploadMultiplePhotos(loginUser.getUserId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "다중 사진 업로드가 완료되었습니다.");
            return "redirect:/feed";
        } catch (UploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/upload/photos";
        }
    }

    @GetMapping("/upload/video")
    public String videoPage(HttpSession session, Model model) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("albums", uploadService.getActiveAlbums(loginUser.getUserId()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new VideoUploadForm());
        }
        return "upload/video";
    }

    @PostMapping("/upload/video")
    public String uploadVideo(@ModelAttribute("form") VideoUploadForm form,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        try {
            uploadService.uploadVideo(loginUser.getUserId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "동영상 업로드가 완료되었습니다.");
            return "redirect:/feed";
        } catch (UploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/upload/video";
        }
    }
}
