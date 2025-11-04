package com.capstone.ai_interview_be.controller.OptionsController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.capstone.ai_interview_be.service.JDScraperService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("/api/jd")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowCredentials = "false") 
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

    //scrape JD text only from URL - NO AI analysis
    @PostMapping("/scrape-url")
    public ResponseEntity<?> scrapeJDFromUrl(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "URL cannot be empty"));
            }
            
            // Scrape JD content from URL - NO AI analysis
            String jdText = jdScraperService.scrapeJDFromUrl(url);
            
            // Return only the scraped text
            return ResponseEntity.ok(Map.of(
                "jdText", jdText,
                "success", true,
                "message", "Job description scraped successfully"
            ));
            
        } catch (java.net.MalformedURLException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid URL format: " + e.getMessage()));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch job description: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
    
    //scan JD from URL - scrape + AI analysis
    @PostMapping("/scan-url")
    public ResponseEntity<?> scanJDFromUrl(@RequestBody Map<String, String> request) {
        try {
            String url = request.get("url");
            
            if (url == null || url.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "URL cannot be empty"));
            }
            
            // Scrape JD content from URL
            String jdText = jdScraperService.scrapeJDFromUrl(url);
            
            // Extract data using AI
            DataScanResponse jdData = aiService.extractData(jdText);
            
            // Include JD text for GenQ service to generate contextual questions
            jdData.setExtractedText(jdText);
            
            return ResponseEntity.ok(jdData);
            
        } catch (java.net.MalformedURLException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid URL format: " + e.getMessage()));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch job description: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
}
