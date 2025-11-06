package com.capstone.ai_interview_be.controller.OptionsController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.capstone.ai_interview_be.service.OptionsService.FileParserService;

import org.springframework.web.bind.annotation.RequestParam;




@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CVController {

    private final FileParserService fileParserService;
    private final AIService aiService;

    // Phương thức để quét và trích xuất dữ liệu từ CV
    @PostMapping("/scan")
    public ResponseEntity<DataScanResponse> scanCV(
        @RequestParam("file") MultipartFile file) {
        try {
            String extractedText = fileParserService.parseCV(file);
            
            DataScanResponse cvData = aiService.extractData(extractedText);
            cvData.setExtractedText(extractedText);
            
            return ResponseEntity.ok(cvData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
