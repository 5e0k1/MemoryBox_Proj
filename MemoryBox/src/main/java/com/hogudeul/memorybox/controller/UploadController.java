package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.model.AlbumOption;
import com.hogudeul.memorybox.model.Tag;
import com.hogudeul.memorybox.service.UploadService;
import com.hogudeul.memorybox.upload.MultiPhotoUploadForm;
import com.hogudeul.memorybox.upload.SinglePhotoUploadForm;
import com.hogudeul.memorybox.upload.UploadException;
import com.hogudeul.memorybox.upload.VideoUploadForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
        model.addAttribute("tags", uploadService.getActiveTags(loginUser.getUserId()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new SinglePhotoUploadForm());
        }
        return "upload/single-photo";
    }

    @PostMapping("/upload/photo")
    public Object uploadSinglePhoto(@ModelAttribute("form") SinglePhotoUploadForm form,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        boolean ajax = isAjaxRequest(request);
        try {
            uploadService.uploadSinglePhoto(loginUser.getUserId(), form);
            if (ajax) {
                return uploadSuccessResponse("사진 1장 업로드가 완료되었습니다.", "/feed");
            }
            redirectAttributes.addFlashAttribute("successMessage", "사진 1장 업로드가 완료되었습니다.");
            return "redirect:/feed";
        } catch (UploadException e) {
            if (ajax) {
                return uploadErrorResponse(e.getMessage(), "/upload/photo");
            }
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
        model.addAttribute("tags", uploadService.getActiveTags(loginUser.getUserId()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new MultiPhotoUploadForm());
        }
        return "upload/multi-photo";
    }

    @PostMapping("/upload/photos")
    public Object uploadMultiPhoto(@ModelAttribute("form") MultiPhotoUploadForm form,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        boolean ajax = isAjaxRequest(request);
        try {
            uploadService.uploadMultiplePhotos(loginUser.getUserId(), form);
            if (ajax) {
                return uploadSuccessResponse("다중 사진 업로드가 완료되었습니다.", "/feed");
            }
            redirectAttributes.addFlashAttribute("successMessage", "다중 사진 업로드가 완료되었습니다.");
            return "redirect:/feed";
        } catch (UploadException e) {
            if (ajax) {
                return uploadErrorResponse(e.getMessage(), "/upload/photos");
            }
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
        model.addAttribute("tags", uploadService.getActiveTags(loginUser.getUserId()));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new VideoUploadForm());
        }
        return "upload/video";
    }

    @PostMapping("/upload/video")
    public Object uploadVideo(@ModelAttribute("form") VideoUploadForm form,
                              HttpSession session,
                              RedirectAttributes redirectAttributes,
                              HttpServletRequest request) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        boolean ajax = isAjaxRequest(request);
        try {
            uploadService.uploadVideo(loginUser.getUserId(), form);
            if (ajax) {
                return uploadSuccessResponse("동영상 업로드가 완료되었습니다.", "/feed");
            }
            redirectAttributes.addFlashAttribute("successMessage", "동영상 업로드가 완료되었습니다.");
            return "redirect:/feed";
        } catch (UploadException e) {
            if (ajax) {
                return uploadErrorResponse(e.getMessage(), "/upload/video");
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/upload/video";
        }
    }


    @PostMapping("/upload/album")
    @ResponseBody
    public Map<String, Object> createAlbum(@RequestParam("albumName") String albumName, HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return result;
        }

        try {
            AlbumOption album = uploadService.createAlbum(albumName);
            result.put("success", true);
            result.put("albumId", album.getAlbumId());
            result.put("albumName", album.getAlbumName());
            return result;
        } catch (UploadException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PostMapping("/upload/tag")
    @ResponseBody
    public Map<String, Object> createTag(@RequestParam("tagName") String tagName, HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return result;
        }

        try {
            Tag tag = uploadService.createOrGetTag(loginUser.getUserId(), tagName);
            result.put("success", true);
            result.put("tagId", tag.getTagId());
            result.put("tagName", tag.getTagName());
            result.put("normalizedName", tag.getNormalizedName());
            return result;
        } catch (UploadException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }

    private ResponseEntity<Map<String, Object>> uploadSuccessResponse(String message, String redirectUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("redirectUrl", redirectUrl);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> uploadErrorResponse(String message, String redirectUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("redirectUrl", redirectUrl);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
