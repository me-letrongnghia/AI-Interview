package com.capstone.ai_interview_be.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
public class OpenTTSService {
    
    private final WebClient webClient;
    private final String defaultVoice;
    private static final int MAX_TEXT_LENGTH = 500; // Giới hạn text
    private static final int TIMEOUT_SECONDS = 30; // Timeout 30 giây
    
    public OpenTTSService(@Value("${opentts.api-url}") String apiUrl,
                          @Value("${opentts.default-voice}") String defaultVoice) {
        this.defaultVoice = defaultVoice;
        
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .build();
        
        log.info("OpenTTS Service initialized with URL: {} and default voice: {}", apiUrl, defaultVoice);
    }
    
    /**
     * Chuyển đổi text thành audio (speech)
     */
    public byte[] textToSpeech(String text, String voice) {
        try {
            // Validate và cắt text nếu quá dài
            if (text == null || text.trim().isEmpty()) {
                log.warn("Empty text provided for TTS");
                return null;
            }
            
            String cleanedText = text.trim();
            if (cleanedText.length() > MAX_TEXT_LENGTH) {
                log.warn("Text too long ({}), truncating to {}", cleanedText.length(), MAX_TEXT_LENGTH);
                cleanedText = cleanedText.substring(0, MAX_TEXT_LENGTH) + "...";
            }
            
            String voiceToUse = (voice != null && !voice.isEmpty()) ? voice : defaultVoice;
            
            log.info("Converting text to speech with voice: {} (text length: {})", voiceToUse, cleanedText.length());
            log.debug("Text content: {}", cleanedText.substring(0, Math.min(100, cleanedText.length())));
            
            String url = UriComponentsBuilder.fromPath("/api/tts")
                    .queryParam("voice", voiceToUse)
                    .queryParam("text", cleanedText)
                    .toUriString();
            
            byte[] audioData = webClient.get()
                    .uri(url)
                    .accept(MediaType.parseMediaType("audio/wav"))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();
            
            // Kiểm tra kỹ response
            if (audioData == null) {
                log.error("Received null audio data from OpenTTS");
                return null;
            }
            
            if (audioData.length == 0) {
                log.error("Received empty audio data (0 bytes) from OpenTTS");
                return null;
            }
            
            // Kiểm tra xem có phải là audio file hợp lệ không (WAV file bắt đầu với "RIFF")
            if (audioData.length < 44) {
                log.error("Audio data too small to be valid WAV file ({} bytes)", audioData.length);
                return null;
            }
            
            log.info("Successfully generated speech audio ({} bytes)", audioData.length);
            return audioData;
            
        } catch (WebClientResponseException e) {
            log.error("OpenTTS API error: {} - {} - Body: {}", 
                e.getStatusCode(), 
                e.getStatusText(),
                e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling OpenTTS API: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Chuyển text thành audio và encode sang Base64 để gửi qua WebSocket
     */
    public String textToSpeechBase64(String text, String voice) {
        try {
            byte[] audioData = textToSpeech(text, voice);
            if (audioData != null && audioData.length > 0) {
                String base64Audio = Base64.getEncoder().encodeToString(audioData);
                log.info("Converted audio to Base64 (original: {} bytes, base64 length: {})", 
                    audioData.length, base64Audio.length());
                return base64Audio;
            }
            log.warn("Cannot convert to Base64: audio data is null or empty");
            return null;
        } catch (Exception e) {
            log.error("Error converting audio to Base64: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Lấy danh sách voices có sẵn
     */
    public String getAvailableVoices() {
        try {
            log.info("Fetching available voices from OpenTTS");
            
            String voices = webClient.get()
                    .uri("/api/voices")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            log.info("Successfully retrieved voices list");
            return voices;
            
        } catch (Exception e) {
            log.error("Error fetching voices from OpenTTS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch available voices: " + e.getMessage(), e);
        }
    }
}