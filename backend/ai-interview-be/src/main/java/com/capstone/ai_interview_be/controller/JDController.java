package com.capstone.ai_interview_be.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.service.AIService.AIService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jd")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowCredentials = "false") 
public class JDController {
    
    private final AIService aiService;

   @PostMapping("/scan")
    public ResponseEntity<DataScanResponse> scanJD(@RequestBody String jdText) {
        DataScanResponse jdData = aiService.extractData(jdText);
        
        // Include JD text for GenQ service to generate contextual questions
        jdData.setExtractedText(jdText);
        
        return ResponseEntity.ok(jdData);
    }
}
