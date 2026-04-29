package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.DetailService;
import com.hogudeul.memorybox.service.ZipDownloadService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ZipDownloadController {

    private static final Logger log = LoggerFactory.getLogger(ZipDownloadController.class);
    private final ZipDownloadService zipDownloadService;

    public ZipDownloadController(ZipDownloadService zipDownloadService) {
        this.zipDownloadService = zipDownloadService;
    }

    @GetMapping("/temp/zip/{fileName}")
    public ResponseEntity<Resource> downloadTempZip(@PathVariable String fileName,
                                                    HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }

        final Path zipPath;
        try {
            zipPath = zipDownloadService.resolveZipForDownload(fileName);
        } catch (DetailService.DownloadException e) {
            if (e.getMessage() != null && e.getMessage().contains("잘못된 파일명")) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.notFound().build();
        }

        long contentLength = -1L;
        try {
            contentLength = Files.size(zipPath);
        } catch (IOException e) {
            log.debug("zip content-length lookup failed. fileName={}, msg={}", fileName, e.getMessage());
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        if (contentLength >= 0) {
            builder.contentLength(contentLength);
        }

        return builder.body(new FileSystemResource(zipPath));
    }
}
