package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/translate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class TranslationController {

    private final GroqAIService groqAIService;

    @PostMapping("/text")
    public ResponseEntity<ApiResponse<Map<String, String>>> translateText(
            @RequestParam String text,
            @RequestParam String targetLanguage) {

        try {
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("EMPTY_TEXT", "Text to translate cannot be empty"));
            }

            if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_LANGUAGE", "Target language is required"));
            }

            // For English, return as-is
            if (targetLanguage.equalsIgnoreCase("en") || targetLanguage.equalsIgnoreCase("english")) {
                Map<String, String> result = new HashMap<>();
                result.put("originalText", text);
                result.put("translatedText", text);
                result.put("language", "en");
                return ResponseEntity.ok(ApiResponse.success("No translation needed for English", result));
            }

            String translatedText;

            // Check if this is a transliteration request (roman_XX format)
            if (targetLanguage.startsWith("roman_")) {
                String sourceLanguage = targetLanguage.substring(6); // Extract language code after "roman_"
                log.info("Transliterating to Roman script from {}", sourceLanguage);
                translatedText = groqAIService.transliterateToRoman(text, sourceLanguage);
            } else {
                // Normal translation request
                log.info("Translating to {}", targetLanguage);
                translatedText = groqAIService.translateText(text, targetLanguage);
            }

            Map<String, String> result = new HashMap<>();
            result.put("originalText", text);
            result.put("translatedText", translatedText);
            result.put("language", targetLanguage);

            log.info("Translation/Transliteration successful for: {}", targetLanguage);
            return ResponseEntity.ok(ApiResponse.success("Text translated successfully", result));

        } catch (Exception e) {
            log.error("Translation error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRANSLATION_ERROR", "Failed to translate text: " + e.getMessage()));
        }
    }
}
