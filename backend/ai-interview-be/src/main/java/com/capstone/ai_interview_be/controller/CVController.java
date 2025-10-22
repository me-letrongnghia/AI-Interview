package com.capstone.ai_interview_be.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.service.FileParserService;
import com.capstone.ai_interview_be.service.AIService.AIService;

import org.springframework.web.bind.annotation.RequestParam;




@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowCredentials = "false") 
public class CVController {

    private final FileParserService fileParserService;
    private final AIService aiService;

    // Endpoint để scan và trích xuất dữ liệu từ CV
    @PostMapping("/scan")
    public ResponseEntity<DataScanResponse> scanCV(
        @RequestParam("file") MultipartFile file) {
        try {
            String extractedText = fileParserService.parseCV(file);
            
            DataScanResponse cvData = aiService.extractData(extractedText);
            
            return ResponseEntity.ok(cvData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint để trích xuất văn bản thô từ CV
    @PostMapping("/text")
    public ResponseEntity<String> textCv(@RequestParam("file") MultipartFile file) {
        try {
            String extractedText = fileParserService.parseCV(file);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(extractedText != null ? extractedText : "NO TEXT EXTRACTED");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error parsing file: " + e.getMessage());
        }
    }
}
