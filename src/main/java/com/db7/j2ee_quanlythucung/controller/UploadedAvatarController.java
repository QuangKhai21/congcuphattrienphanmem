package com.db7.j2ee_quanlythucung.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Phục vụ ảnh avatar user từ đúng thư mục {@code file.upload-dir/avatars},
 * trùng với nơi {@link ProfileController} ghi file — tránh lệch với static resource handler.
 */
@RestController
public class UploadedAvatarController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/uploads/avatars/{filename:.+}")
    public ResponseEntity<Resource> serveUserAvatar(@PathVariable String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        Path base = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        Path file = base.resolve(filename).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                mediaType = MediaType.parseMediaType(probed);
            }
        } catch (Exception ignored) {
            /* dùng octet-stream */
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.replace("\"", "") + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}
