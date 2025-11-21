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

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("File is empty");
        }

        // Kiểm tra dung lượng < 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("File too large (max 5MB)");
        }

        // Kiểm tra MIME type
        String contentType = file.getContentType();
        if (!List.of("image/jpeg", "image/png", "image/jpg").contains(contentType)) {
            return ResponseEntity.badRequest().body("Only JPG/PNG files allowed");
        }

       try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto")
            );
            String imageUrl = (String) uploadResult.get("secure_url");
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));

        } catch (IOException e) {
            // LỖI QUAN TRỌNG: Thường xảy ra khi đọc file.getBytes()
            log.error("Lỗi IO khi upload file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Lỗi khi đọc file: " + e.getMessage());

        } catch (Exception e) {
            log.error("!!! LỖI UPLOAD: Đã xảy ra lỗi không xác định.", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Upload thất bại do lỗi máy chủ: " + e.getMessage());
        }
    }

    // Optional: API sinh signature (nếu frontend muốn upload trực tiếp)
    // @Value("${cloudinary.api_secret}")
    // private String apiSecret;

    // @Value("${cloudinary.api_key}")
    // private String apiKey;

    // @Value("${cloudinary.cloud_name}")
    // private String cloudName;

    // @PostMapping("/signature")
    // public ResponseEntity<?> getSignature() {
    //     long timestamp = System.currentTimeMillis() / 1000;
    //     String signature = com.cloudinary.utils.SignatureUtils.apiSignRequest(
    //             Map.of("timestamp", String.valueOf(timestamp)),
    //             apiSecret
    //     );

    //     return ResponseEntity.ok(Map.of(
    //             "timestamp", String.valueOf(timestamp),
    //             "signature", signature,
    //             "apiKey", apiKey,
    //             "cloudName", cloudName
    //     ));
    // }
}
