package com.capstone.ai_interview_be.controller.OptionsController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.capstone.ai_interview_be.service.OptionsService.JDScraperService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("/api/jd")
@RequiredArgsConstructor
public class JDController {
    
    private final AIService aiService;
    private final JDScraperService jdScraperService;

    // Phương thức để quét và trích xuất dữ liệu từ JD
   @PostMapping("/scan")
    public ResponseEntity<DataScanResponse> scanJD(@RequestBody String jdText) {
        DataScanResponse jdData = aiService.extractData(jdText);
        jdData.setExtractedText(jdText);
        
        return ResponseEntity.ok(jdData);
    }
    
    // Phương thức để quét và trích xuất dữ liệu từ JD qua URL
    @PostMapping("/scan-url")
    public ResponseEntity<?> scanJDFromUrl(@RequestBody Map<String, String> request) {
        try {
            // Lấy URL từ yêu cầu
            String url = request.get("url");
            // Kiểm tra URL hợp lệ không?
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "URL cannot be empty"));
            }
            
            // Trích xuất JD từ URL
            String jdText = jdScraperService.scrapeJDFromUrl(url);
            
            return ResponseEntity.ok(jdText);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to scrape JD from URL: " + e.getMessage()));
        }
    }
}
