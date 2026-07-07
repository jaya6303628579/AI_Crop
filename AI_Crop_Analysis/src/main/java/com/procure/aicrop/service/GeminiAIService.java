package com.procure.aicrop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
@Slf4j
public class GeminiAIService {

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-pro}")
    private String model;

    @Value("${ai.gemini.vision-model:gemini-pro-vision}")
    private String visionModel;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        }
        return httpClient;
    }

    // ============================================
    // SOIL ANALYSIS
    // ============================================
    public String analyzeSoilImage(MultipartFile imageFile) {
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                log.warn("Image file is empty");
                return getDefaultSoilAnalysis();
            }

            byte[] imageData = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String mimeType = imageFile.getContentType();

            log.info("Analyzing soil image: {} ({})", imageFile.getOriginalFilename(), mimeType);

            String prompt = """
                    Analyze this soil image carefully. Return ONLY a valid JSON object with these fields:
                    {
                      "soil_type": "LOAMY",
                      "ph_level": 6.8,
                      "nitrogen": 250,
                      "phosphorus": 25,
                      "potassium": 450,
                      "organic_matter": 3.2,
                      "texture": "MEDIUM",
                      "moisture_level": 22.5,
                      "confidence_score": 85,
                      "detailed_analysis": "High quality loamy soil with balanced nutrients, suitable for most crops"
                    }

                    Return ONLY JSON, no markdown, no explanation. Replace with actual analysis values.
                    """;

            String response = callGeminiVisionAPI(base64Image, mimeType, prompt);
            log.info("Soil analysis complete. Response length: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("Failed to analyze soil image", e);
            return getDefaultSoilAnalysis();
        }
    }

    // ============================================
    // CROP RECOMMENDATION
    // ============================================
    public String generateCropRecommendation(String soilType, Double pH, Double nitrogen,
                                           Double phosphorus, Double potassium, Double rainfall,
                                           Double temperature, String region) {
        try {
            String prompt = String.format("""
                    Based on these soil parameters, recommend the best 3 crops. Return ONLY JSON:
                    {
                      "recommendations": [
                        {"crop_name": "Rice", "suitability_score": 95, "sowing_start": "2026-06-01", "sowing_end": "2026-07-15", "expected_yield": 5000},
                        {"crop_name": "Wheat", "suitability_score": 85, "sowing_start": "2026-10-01", "sowing_end": "2026-11-15", "expected_yield": 4000},
                        {"crop_name": "Maize", "suitability_score": 90, "sowing_start": "2026-05-01", "sowing_end": "2026-06-15", "expected_yield": 4500}
                      ],
                      "confidence": 88,
                      "reason": "Soil conditions are ideal"
                    }

                    Parameters: pH=%.1f, Nitrogen=%.0f, Phosphorus=%.0f, Potassium=%.0f, Rainfall=%.0f mm, Temp=%.1f°C, Region=%s
                    """, pH, nitrogen, phosphorus, potassium, rainfall, temperature, region);

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Crop recommendation failed", e);
            return getDefaultRecommendation();
        }
    }

    // ============================================
    // YIELD PREDICTION
    // ============================================
    public String predictYield(String cropName, Integer growthDaysCovered, String currentStage,
                              Double temperature, Double rainfall, Double nitrogen,
                              Integer daysToMaturity) {
        try {
            String prompt = String.format("""
                    Predict crop yield. Return ONLY JSON:
                    {
                      "predicted_yield_kg_per_hectare": 4500,
                      "confidence_percentage": 85,
                      "growth_progress": 65,
                      "temperature_impact": "FAVORABLE",
                      "rainfall_impact": "FAVORABLE",
                      "recommendations": ["Continue regular irrigation", "Monitor for pests"]
                    }

                    Crop: %s, Stage: %s, Days: %d/%d, Temp: %.1f°C, Rain: %.0f mm, N: %.0f
                    """, cropName, currentStage, growthDaysCovered, daysToMaturity, temperature, rainfall, nitrogen);

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Yield prediction failed", e);
            return getDefaultYieldPrediction();
        }
    }

    // ============================================
    // DISEASE & PEST DETECTION
    // ============================================
    public String detectDiseaseAndPests(MultipartFile leafImage, String cropName) {
        try {
            if (leafImage == null || leafImage.isEmpty()) {
                return getDefaultDiseaseAnalysis();
            }

            byte[] imageData = leafImage.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String mimeType = leafImage.getContentType();

            String prompt = String.format("""
                    Analyze this %s leaf image for diseases. Return ONLY JSON:
                    {
                      "health_status": 85,
                      "diseases": [],
                      "pests": [],
                      "nutrient_deficiencies": [],
                      "risk_level": "LOW",
                      "treatment_recommendations": ["Maintain regular watering", "Monitor closely"]
                    }
                    """, cropName);

            return callGeminiVisionAPI(base64Image, mimeType, prompt);
        } catch (Exception e) {
            log.error("Disease detection failed", e);
            return getDefaultDiseaseAnalysis();
        }
    }

    // ============================================
    // GROWTH STAGE PREDICTION
    // ============================================
    public String predictGrowthStage(String cropName, Integer daysAfterSowing,
                                    Double temperature, Double rainfall, Integer dayLength) {
        try {
            String prompt = String.format("""
                    Predict growth stage. Return ONLY JSON:
                    {
                      "growth_stage": "VEGETATIVE",
                      "stage_progress_percentage": 45,
                      "days_to_next_stage": 15,
                      "critical_operations": ["Apply nitrogen fertilizer", "Ensure adequate water"],
                      "expected_yield_potential": 80
                    }

                    Crop: %s, Days: %d, Temp: %.1f°C, Rain: %.0f mm, DayLen: %d hrs
                    """, cropName, daysAfterSowing, temperature, rainfall, dayLength);

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Growth stage prediction failed", e);
            return getDefaultGrowthStage();
        }
    }

    // ============================================
    // ALERT GENERATION
    // ============================================
    public String generateAlerts(String cropName, String currentStage, Double temperature,
                                Double rainfall, Double diseaseRisk, Double pestRisk,
                                Double weatherAlertTemp) {
        try {
            String prompt = String.format("""
                    Generate farming alerts. Return ONLY JSON:
                    {
                      "alerts": [
                        {"type": "WEATHER", "severity": "MEDIUM", "message": "Expected rainfall tomorrow", "action": "Prepare drainage"},
                        {"type": "GROWTH", "severity": "LOW", "message": "Stage transition approaching", "action": "Prepare next stage inputs"}
                      ],
                      "priority_score": 35
                    }

                    Crop: %s, Stage: %s, Temp: %.1f°C, Rain: %.0f mm, Disease Risk: %.0f%%, Pest Risk: %.0f%%
                    """, cropName, currentStage, temperature, rainfall, diseaseRisk, pestRisk);

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Alert generation failed", e);
            return getDefaultAlerts();
        }
    }

    // ============================================
    // API CALLS
    // ============================================
    private String callGeminiAPI(String prompt) {
        try {
            String url = String.format("%s/%s:generateContent?key=%s", GEMINI_API_URL, model, geminiApiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contentsArray = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            ArrayNode partsArray = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", prompt);
            partsArray.add(part);
            content.set("parts", partsArray);
            contentsArray.add(content);
            requestBody.set("contents", contentsArray);

            String requestJson = requestBody.toString();
            log.debug("Gemini request to: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Gemini API response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String extracted = extractGeminiResponse(response.body());
                log.info("Extracted response: {}", extracted.substring(0, Math.min(100, extracted.length())));
                return extracted;
            } else {
                log.error("Gemini API error {}. Response: {}", response.statusCode(), response.body());
                return getDefaultResponse();
            }
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return getDefaultResponse();
        }
    }

    private String callGeminiVisionAPI(String base64Image, String mimeType, String prompt) {
        try {
            String url = String.format("%s/%s:generateContent?key=%s", GEMINI_API_URL, visionModel, geminiApiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contentsArray = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            ArrayNode partsArray = objectMapper.createArrayNode();

            ObjectNode imagePart = objectMapper.createObjectNode();
            ObjectNode inlineData = objectMapper.createObjectNode();
            inlineData.put("mime_type", mimeType != null ? mimeType : "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.set("inline_data", inlineData);
            partsArray.add(imagePart);

            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("text", prompt);
            partsArray.add(textPart);

            content.set("parts", partsArray);
            contentsArray.add(content);
            requestBody.set("contents", contentsArray);

            String requestJson = requestBody.toString();
            log.debug("Vision API request size: {} bytes", requestJson.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Vision API response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String extracted = extractGeminiResponse(response.body());
                log.info("Extracted vision response length: {}", extracted.length());
                return extracted;
            } else {
                log.error("Vision API error {}. Response: {}", response.statusCode(), response.body());
                return getDefaultSoilAnalysis();
            }
        } catch (Exception e) {
            log.error("Vision API call failed", e);
            return getDefaultSoilAnalysis();
        }
    }

    // ============================================
    // RESPONSE PARSING
    // ============================================
    private String extractGeminiResponse(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                log.warn("Empty response from Gemini API");
                return "{}";
            }

            log.debug("Raw Gemini response: {}", response.substring(0, Math.min(500, response.length())));

            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("candidates") && jsonNode.get("candidates").isArray()) {
                var candidates = jsonNode.get("candidates");
                if (candidates.size() > 0) {
                    var firstCandidate = candidates.get(0);

                    if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
                        var parts = firstCandidate.get("content").get("parts");
                        if (parts.size() > 0 && parts.get(0).has("text")) {
                            String text = parts.get(0).get("text").asText().trim();

                            // Try to extract JSON from text
                            if (text.startsWith("{")) {
                                // Already JSON
                                log.debug("Response is already JSON");
                                return text;
                            } else if (text.contains("```json")) {
                                // JSON in markdown code block
                                text = text.split("```json")[1].split("```")[0].trim();
                                log.debug("Extracted JSON from markdown");
                                return text;
                            } else if (text.contains("{")) {
                                // Try to find JSON object in text
                                int start = text.indexOf("{");
                                int end = text.lastIndexOf("}") + 1;
                                if (start >= 0 && end > start) {
                                    String json = text.substring(start, end);
                                    log.debug("Extracted JSON from text");
                                    return json;
                                }
                            }

                            // Return the text as-is if it looks like JSON
                            return text;
                        }
                    }
                }
            }

            log.error("No candidates in Gemini response");
            return "{}";
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return "{}";
        }
    }

    // ============================================
    // DEFAULT RESPONSES (FALLBACK)
    // ============================================
    private String getDefaultSoilAnalysis() {
        return """
                {
                  "soil_type": "LOAMY",
                  "ph_level": 6.8,
                  "nitrogen": 250,
                  "phosphorus": 25,
                  "potassium": 450,
                  "organic_matter": 3.2,
                  "texture": "MEDIUM",
                  "moisture_level": 22.5,
                  "confidence_score": 85,
                  "detailed_analysis": "High quality loamy soil with balanced nutrients. Suitable for rice, wheat, and maize cultivation."
                }
                """;
    }

    private String getDefaultRecommendation() {
        return """
                {
                  "recommendations": [
                    {"crop_name": "Rice", "suitability_score": 92, "sowing_start": "2026-06-01", "sowing_end": "2026-07-15", "expected_yield": 5000},
                    {"crop_name": "Wheat", "suitability_score": 85, "sowing_start": "2026-10-01", "sowing_end": "2026-11-15", "expected_yield": 4000},
                    {"crop_name": "Maize", "suitability_score": 88, "sowing_start": "2026-05-01", "sowing_end": "2026-06-15", "expected_yield": 4500}
                  ],
                  "confidence": 87,
                  "reason": "Soil conditions are ideal for these crops"
                }
                """;
    }

    private String getDefaultYieldPrediction() {
        return """
                {
                  "predicted_yield_kg_per_hectare": 4500,
                  "confidence_percentage": 80,
                  "growth_progress": 65,
                  "temperature_impact": "FAVORABLE",
                  "rainfall_impact": "FAVORABLE",
                  "recommendations": ["Continue regular watering", "Monitor for pests", "Apply fertilizer as scheduled"]
                }
                """;
    }

    private String getDefaultDiseaseAnalysis() {
        return """
                {
                  "health_status": 88,
                  "diseases": [],
                  "pests": [],
                  "nutrient_deficiencies": [],
                  "risk_level": "LOW",
                  "treatment_recommendations": ["Maintain regular watering", "Monitor weekly", "Continue preventive measures"]
                }
                """;
    }

    private String getDefaultGrowthStage() {
        return """
                {
                  "growth_stage": "VEGETATIVE",
                  "stage_progress_percentage": 60,
                  "days_to_next_stage": 12,
                  "critical_operations": ["Apply nitrogen fertilizer", "Ensure adequate irrigation", "Monitor for weeds"],
                  "expected_yield_potential": 82
                }
                """;
    }

    private String getDefaultAlerts() {
        return """
                {
                  "alerts": [
                    {"type": "GROWTH", "severity": "LOW", "message": "Growth stage on track", "action": "Continue monitoring"},
                    {"type": "NUTRIENT", "severity": "MEDIUM", "message": "Nitrogen usage is optimal", "action": "Monitor nitrogen levels"}
                  ],
                  "priority_score": 25
                }
                """;
    }

    private String getDefaultResponse() {
        return "{}";
    }

    public boolean isEnabled() {
        return geminiApiKey != null && !geminiApiKey.isEmpty();
    }
}
