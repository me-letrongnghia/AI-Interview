package com.capstone.ai_interview_be.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cloudinary")
@Slf4j
@RequiredArgsConstructor
public class CloudinaryController {
    private final Cloudinary cloudinary;

    // Phương thức upload ảnh lên Cloudinary
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        // Kiểm tra file rỗng
        if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("File is empty");
        }

        // Kiểm tra dung lượng < 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("File too large (max 5MB)");
        }

        // Kiểm tra định dạng file
        String contentType = file.getContentType();
        if (!List.of("image/jpeg", "image/png", "image/jpg").contains(contentType)) {
            return ResponseEntity.badRequest().body("Only JPG/PNG files allowed");
        }

       try {
            // Upload file lên Cloudinary   
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto")
            );
            // Lấy URL ảnh từ kết quả upload
            String imageUrl = (String) uploadResult.get("secure_url");
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));

        } catch (IOException e) {
            log.error("Error reading file for upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Lỗi khi đọc file: " + e.getMessage());

        } catch (Exception e) {
            log.error("Error uploading file to Cloudinary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error uploading file: " + e.getMessage());
        }
    }

}
