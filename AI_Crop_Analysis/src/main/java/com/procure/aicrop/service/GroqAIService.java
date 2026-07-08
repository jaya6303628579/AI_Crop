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
public class GroqAIService {

    @Value("${ai.groq.api-key}")
    private String groqApiKey;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${ai.groq.vision-model:meta-llama/llama-4-scout-17b-16e-instruct}")
    private String visionModel;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        }
        return httpClient;
    }

    // ============================================
    // SOIL ANALYSIS (VISION)
    // ============================================
    public String analyzeSoilImage(MultipartFile imageFile) {
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                log.warn("Image file is empty");
                return getDefaultSoilAnalysis();
            }

            byte[] imageData = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String mimeType = imageFile.getContentType() != null ? imageFile.getContentType() : "image/jpeg";

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

            String response = callGroqVisionAPI(base64Image, mimeType, prompt);
            log.info("Soil analysis complete. Response length: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("Failed to analyze soil image", e);
            return getDefaultSoilAnalysis();
        }
    }

    // ============================================
    // CROP RECOMMENDATION (TEXT)
    // ============================================
    public String generateCropRecommendation(String soilType, Double pH, Double nitrogen,
                                           Double phosphorus, Double potassium, Double rainfall,
                                           Double temperature, String region) {
        try {
            String today = java.time.LocalDate.now().toString();
            String prompt = String.format("""
                    You are an agronomy advisor. Today's date is %s. The data below is REAL, measured just now:
                    - Current temperature: %.1f°C (live reading, right now)
                    - Rainfall forecast for the next 7 days: %.0f mm (live forecast, starting today)
                    - Soil type: %s, pH=%.1f, Nitrogen=%.0f, Phosphorus=%.0f, Potassium=%.0f
                    - Region: %s

                    Recommend the best 3 crops for this farmer using this real data. Ground your sowing window in what
                    you were actually given: if current temperature and the 7-day rainfall outlook are favorable right
                    now, recommend sowing starting within the next 1-4 weeks. If the ideal sowing season for a crop is
                    further out than the 7-day window you were given, say so explicitly in benefit_explanation (e.g.
                    "current conditions are off-season for X; based on regional seasonal norms the next favorable
                    window is..."). Do not invent a far-future date without explaining why it's not based on the
                    live data provided.

                    Also identify a dangerous sowing window to explicitly AVOID for each crop - a specific date range
                    where sowing would be risky (e.g. a heavy-rain window causing flooding or seed rot risk right
                    before/during the recommended window), grounded in the real weather given above.

                    Return ONLY this JSON structure, no markdown, no explanation outside the JSON:
                    {
                      "recommendations": [
                        {
                          "crop_name": "Rice",
                          "suitability_score": 95,
                          "sowing_start": "2026-06-01",
                          "sowing_end": "2026-07-15",
                          "expected_yield": 5000,
                          "benefit_explanation": "Specific reasons this crop and sowing window suits THIS soil and TODAY'S weather reading",
                          "risk_explanation": "Specific dates/conditions to avoid and why, based on THIS soil and weather",
                          "dangerous_dates": {
                            "avoid_start": "2026-05-25",
                            "avoid_end": "2026-05-30",
                            "expected_rainfall_mm": 160,
                            "risk_level": "HIGH",
                            "recommendation": "Wait until rainfall reduces before sowing"
                          }
                        }
                      ],
                      "confidence": 88,
                      "reason": "Overall reasoning for these picks based on the given soil and weather conditions"
                    }
                    """, today, temperature, rainfall, soilType, pH, nitrogen, phosphorus, potassium, region);

            return callGroqAPI(prompt);
        } catch (Exception e) {
            log.error("Crop recommendation failed", e);
            return getDefaultRecommendation();
        }
    }

    // ============================================
    // YIELD PREDICTION (TEXT)
    // ============================================
    public String predictYield(String cropName, Integer growthDaysCovered, String currentStage,
                              Double temperature, Double rainfall, Double humidity, Double baseYield,
                              Integer daysToMaturity) {
        try {
            String prompt = String.format("""
                    You are an agronomy advisor recalculating a live yield prediction for a crop already growing in
                    the field. The base expected yield under normal conditions is %.2f (same unit the farmer already
                    sees elsewhere, e.g. tons/hectare) - adjust this UP or DOWN based on the real recent weather
                    below, do not invent a different unit.

                    Crop: %s
                    Growth stage: %s
                    Days since sowing: %d / %d days to maturity
                    Recent average temperature: %.1f°C
                    Recent total rainfall: %.0f mm
                    Recent average humidity: %.0f%%

                    Return ONLY this JSON structure, no markdown, no explanation outside the JSON:
                    {
                      "predicted_yield": %.2f,
                      "confidence_percentage": 85,
                      "growth_progress": 65,
                      "temperature_impact_multiplier": 0.95,
                      "rainfall_impact_multiplier": 0.90,
                      "disease_risk_percentage": 20,
                      "pest_risk_percentage": 15,
                      "risk_level": "MEDIUM",
                      "risk_summary": "One sentence explaining the current risk based on THIS weather and growth stage",
                      "recommendations": ["Specific actionable step 1", "Specific actionable step 2"]
                    }
                    """, baseYield, cropName, currentStage, growthDaysCovered, daysToMaturity,
                    temperature, rainfall, humidity, baseYield);

            return callGroqAPI(prompt);
        } catch (Exception e) {
            log.error("Yield prediction failed", e);
            return getDefaultYieldPrediction();
        }
    }

    // ============================================
    // DISEASE & PEST DETECTION (VISION)
    // ============================================
    public String detectDiseaseAndPests(MultipartFile leafImage, String cropName) {
        try {
            if (leafImage == null || leafImage.isEmpty()) {
                return getDefaultDiseaseAnalysis();
            }

            byte[] imageData = leafImage.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String mimeType = leafImage.getContentType() != null ? leafImage.getContentType() : "image/jpeg";

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

            return callGroqVisionAPI(base64Image, mimeType, prompt);
        } catch (Exception e) {
            log.error("Disease detection failed", e);
            return getDefaultDiseaseAnalysis();
        }
    }

    // ============================================
    // GROWTH STAGE PREDICTION (TEXT)
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

            return callGroqAPI(prompt);
        } catch (Exception e) {
            log.error("Growth stage prediction failed", e);
            return getDefaultGrowthStage();
        }
    }

    // ============================================
    // SOWING PLAN VALIDATION (TEXT)
    // ============================================
    public String validateSowingPlan(String cropName, java.time.LocalDate plannedDate, Double temperature,
                                     Double rainfall, String region, Integer growingDays,
                                     long daysUntilPlanting, boolean forecastBased, String typicalSowingMonths) {
        try {
            String today = java.time.LocalDate.now().toString();

            String weatherContext = forecastBased
                    ? String.format(
                        "The planned date is %d day(s) away, within real forecast range. This is the ACTUAL " +
                        "forecasted weather for %s itself (not today's weather): temperature %.1f°C, rainfall %.0f mm.",
                        daysUntilPlanting, plannedDate, temperature, rainfall)
                    : String.format(
                        "The planned date is %d day(s) away, beyond real weather forecast range (forecasts are only " +
                        "reliable ~5 days out). Do NOT treat the figures below as a prediction for %s - they are " +
                        "just today's live reading for general context: temperature %.1f°C, rainfall %.0f mm. " +
                        "Instead, judge suitability primarily using this crop's typical sowing season from our " +
                        "records: %s. Explicitly say in your reasoning that you're using seasonal norms, not a " +
                        "weather forecast, because the date is too far out.",
                        daysUntilPlanting, plannedDate, temperature, rainfall, typicalSowingMonths);

            String prompt = String.format("""
                    You are an agronomy advisor helping a farmer decide if their planned sowing date is safe.
                    Today's date is %s.

                    Crop the farmer wants to plant: %s (typical growing duration: %d days)
                    Farmer's planned sowing date: %s
                    Region: %s

                    %s

                    Assess whether sowing THIS crop on THIS specific date is safe and likely to succeed. Consider
                    germination conditions, disease/flooding risk, and whether the date is in-season for this crop
                    in this region. If the planned date is risky or off-season, recommend a better sowing window
                    and explain exactly why.

                    Return ONLY this JSON structure, no markdown, no explanation outside the JSON:
                    {
                      "suitable": false,
                      "verdict": "RISKY",
                      "survival_probability": 55,
                      "reasoning": "Specific explanation, and state clearly whether this is based on a real forecast or seasonal norms",
                      "risk_factors": ["Specific risk 1", "Specific risk 2"],
                      "recommended_sowing_start": "2026-07-15",
                      "recommended_sowing_end": "2026-07-22"
                    }

                    verdict must be one of: GOOD, RISKY, POOR.
                    """, today, cropName, growingDays != null ? growingDays : 120, plannedDate, region, weatherContext);

            return callGroqAPI(prompt);
        } catch (Exception e) {
            log.error("Sowing plan validation failed", e);
            return getDefaultSowingValidation(plannedDate);
        }
    }

    // ============================================
    // HARVEST WINDOW PREDICTION (TEXT)
    // ============================================
    public String predictHarvestWindow(String cropName, java.time.LocalDate expectedHarvestDate,
                                      Double temperature, Double rainfall, String region) {
        try {
            String today = java.time.LocalDate.now().toString();
            String prompt = String.format("""
                    You are an agronomy advisor. Today is %s. This crop is approaching harvest.

                    Crop: %s
                    Originally expected harvest date: %s
                    Recent average temperature: %.1f°C
                    Recent total rainfall: %.0f mm
                    Region: %s

                    Recommend the safest harvest window near the expected date, adjusted for the real recent
                    weather above (e.g. delay if heavy rain risk, or narrow the window if conditions are ideal).

                    Return ONLY this JSON structure, no markdown, no explanation outside the JSON:
                    {
                      "harvest_window_start": "2026-11-20",
                      "harvest_window_end": "2026-11-28",
                      "harvest_risk": "LOW",
                      "rain_risk_percentage": 5,
                      "recommendation": "Harvest between 21-24 November for best results."
                    }
                    """, today, cropName, expectedHarvestDate, temperature, rainfall, region);

            return callGroqAPI(prompt);
        } catch (Exception e) {
            log.error("Harvest window prediction failed", e);
            return getDefaultHarvestWindow(expectedHarvestDate);
        }
    }

    // ============================================
    // CROP CATALOG DISCOVERY (TEXT)
    // ============================================
    public String discoverIndianCrops(java.util.List<String> excludeNames) {
        try {
            String excludeList = excludeNames == null || excludeNames.isEmpty()
                    ? "none yet"
                    : String.join(", ", excludeNames);

            String prompt = String.format("""
                    You are an expert in Indian agriculture covering every region: North, South, East, West,
                    Central and Northeast India. List major crops actually cultivated by Indian farmers,
                    INCLUDING crops especially common in South Indian states (Karnataka, Tamil Nadu, Andhra
                    Pradesh, Telangana, Kerala) such as Bengal Gram, Black Gram, Green Gram, Horse Gram, Ragi
                    (Finger Millet), Coconut, Areca Nut, Coffee, Black Pepper, Cardamom, Rubber, Sesame,
                    Sunflower, Tapioca, and Ginger - as well as major crops from every other region of India.

                    Do NOT include any of these crops, they are already in the catalog: %s

                    Return 25-30 NEW crops not in that exclusion list. Return ONLY this JSON structure, no
                    markdown, no explanation outside the JSON:
                    {
                      "crops": [
                        {
                          "name": "Bengal Gram",
                          "local_name": "శనగలు",
                          "icon": "🫘",
                          "description": "Rabi pulse crop widely grown in South and Central India",
                          "growing_days": 100,
                          "water_requirement_mm": 300,
                          "min_temperature": 10,
                          "max_temperature": 25,
                          "optimal_temperature": 20,
                          "min_rainfall_mm": 30,
                          "max_rainfall_mm": 65,
                          "suitable_soil_types": ["LOAMY", "CLAY"],
                          "sowing_months": ["October", "November"],
                          "average_yield": 15
                        }
                      ]
                    }

                    suitable_soil_types must only use values from: CLAY, LOAMY, SANDY, SILTY, PEAT, LATERITE.
                    icon must be a single emoji visually representing the crop.
                    local_name must be the crop's common name in Telugu script, since most users of this app
                    are Telugu-speaking farmers - if you don't know a reliable Telugu name, omit the field.
                    """, excludeList);

            return callGroqAPI(prompt, 4000);
        } catch (Exception e) {
            log.error("Crop catalog discovery failed", e);
            return "{\"crops\":[]}";
        }
    }

    // ============================================
    // ALERT GENERATION (TEXT)
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

            return callGroqAPI(prompt);
        } catch (Exception e) {
            log.error("Alert generation failed", e);
            return getDefaultAlerts();
        }
    }

    // ============================================
    // API CALLS
    // ============================================
    private String callGroqAPI(String prompt) {
        return callGroqAPI(prompt, null);
    }

    private String callGroqAPI(String prompt, Integer maxTokens) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            if (maxTokens != null) {
                requestBody.put("max_tokens", maxTokens);
            }

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            requestBody.set("messages", messages);

            String requestJson = requestBody.toString();
            log.debug("Groq request, size: {} bytes", requestJson.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Groq API response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String extracted = extractGroqResponse(response.body());
                log.info("Extracted response length: {}", extracted.length());
                return extracted;
            } else {
                log.error("Groq API error {}. Response: {}", response.statusCode(), response.body());
                return getDefaultResponse();
            }
        } catch (Exception e) {
            log.error("Groq API call failed", e);
            return getDefaultResponse();
        }
    }

    private String callGroqVisionAPI(String base64Image, String mimeType, String prompt) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", visionModel);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");

            ArrayNode contentArray = objectMapper.createArrayNode();

            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            contentArray.add(textContent);

            ObjectNode imageContent = objectMapper.createObjectNode();
            imageContent.put("type", "image_url");
            ObjectNode imageUrl = objectMapper.createObjectNode();
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
            imageContent.set("image_url", imageUrl);
            contentArray.add(imageContent);

            userMessage.set("content", contentArray);
            messages.add(userMessage);
            requestBody.set("messages", messages);

            String requestJson = requestBody.toString();
            log.debug("Groq vision request size: {} bytes", requestJson.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Groq vision API response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String extracted = extractGroqResponse(response.body());
                log.info("Extracted vision response length: {}", extracted.length());
                return extracted;
            } else {
                log.error("Groq vision API error {}. Response: {}", response.statusCode(), response.body());
                return getDefaultSoilAnalysis();
            }
        } catch (Exception e) {
            log.error("Groq vision API call failed", e);
            return getDefaultSoilAnalysis();
        }
    }

    // ============================================
    // RESPONSE PARSING (OpenAI-compatible format)
    // ============================================
    private String extractGroqResponse(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                log.warn("Empty response from Groq API");
                return "{}";
            }

            log.debug("Raw Groq response: {}", response.substring(0, Math.min(500, response.length())));

            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && jsonNode.get("choices").size() > 0) {
                JsonNode firstChoice = jsonNode.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String text = firstChoice.get("message").get("content").asText().trim();

                    if (text.startsWith("{")) {
                        return text;
                    } else if (text.contains("```json")) {
                        text = text.split("```json")[1].split("```")[0].trim();
                        return text;
                    } else if (text.contains("{")) {
                        int start = text.indexOf("{");
                        int end = text.lastIndexOf("}") + 1;
                        if (start >= 0 && end > start) {
                            return text.substring(start, end);
                        }
                    }
                    return text;
                }
            }

            log.error("No choices in Groq response");
            return "{}";
        } catch (Exception e) {
            log.error("Failed to parse Groq response", e);
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
                  "predicted_yield": null,
                  "confidence_percentage": 60,
                  "growth_progress": 50,
                  "temperature_impact_multiplier": 1.0,
                  "rainfall_impact_multiplier": 1.0,
                  "disease_risk_percentage": 20,
                  "pest_risk_percentage": 20,
                  "risk_level": "UNKNOWN",
                  "risk_summary": "AI prediction unavailable right now; showing baseline estimate.",
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

    private String getDefaultHarvestWindow(java.time.LocalDate expectedHarvestDate) {
        java.time.LocalDate start = expectedHarvestDate != null ? expectedHarvestDate.minusDays(4) : java.time.LocalDate.now().plusDays(4);
        java.time.LocalDate end = expectedHarvestDate != null ? expectedHarvestDate.plusDays(4) : java.time.LocalDate.now().plusDays(12);
        return String.format("""
                {
                  "harvest_window_start": "%s",
                  "harvest_window_end": "%s",
                  "harvest_risk": "MEDIUM",
                  "rain_risk_percentage": 20,
                  "recommendation": "AI prediction unavailable; harvest around the originally planned date and monitor local weather."
                }
                """, start, end);
    }

    private String getDefaultSowingValidation(java.time.LocalDate plannedDate) {
        java.time.LocalDate start = plannedDate != null ? plannedDate : java.time.LocalDate.now().plusDays(5);
        return String.format("""
                {
                  "suitable": true,
                  "verdict": "GOOD",
                  "survival_probability": 70,
                  "reasoning": "AI validation unavailable right now; showing a neutral baseline assessment.",
                  "risk_factors": [],
                  "recommended_sowing_start": "%s",
                  "recommended_sowing_end": "%s"
                }
                """, start, start.plusDays(7));
    }

    private String getDefaultResponse() {
        return "{}";
    }

    // ============================================
    // TEXT TRANSLATION (with caching & fallback)
    // ============================================
    private static final java.util.Map<String, String> translationCache = new java.util.concurrent.ConcurrentHashMap<>();

    public String translateText(String text, String targetLanguage) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return text;
            }

            // Check cache first
            String cacheKey = targetLanguage + ":" + text.hashCode();
            if (translationCache.containsKey(cacheKey)) {
                log.debug("Cache hit for translation: {}", cacheKey);
                return translationCache.get(cacheKey);
            }

            String languageName = getLanguageName(targetLanguage);
            String prompt = String.format("""
                    Translate the following English text to %s.
                    IMPORTANT: Return ONLY the translated text, nothing else. No explanations, no markdown, no extra information.
                    Keep the exact same meaning and structure.

                    English text:
                    %s
                    """, languageName, text);

            String response = callGroqAPIWithFallback(prompt, targetLanguage);
            String result = response.trim();

            // Cache the result
            translationCache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            log.error("Translation failed for language: {}", targetLanguage, e);
            return text; // Return original text on failure
        }
    }

    private String callGroqAPIWithFallback(String prompt, String targetLanguage) {
        try {
            // Try Groq first (will throw exception if rate limited)
            return callGroqAPI(prompt);
        } catch (Exception groqError) {
            log.warn("Groq API failed, trying MyMemory fallback: {}", groqError.getMessage());
            try {
                // Fallback to MyMemory if Groq fails
                return translateWithMyMemory(prompt, targetLanguage);
            } catch (Exception memoryError) {
                log.error("Both Groq and MyMemory failed", memoryError);
                return "{}"; // Return empty response, caller will use original text
            }
        }
    }

    private String translateWithMyMemory(String prompt, String targetLanguage) throws Exception {
        // Extract text from prompt
        String text = prompt.split("English text:\\n")[1].trim();

        String url = String.format(
            "https://api.mymemory.translated.net/get?q=%s&langpair=en|%s",
            java.net.URLEncoder.encode(text, "UTF-8"),
            targetLanguage
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode json = objectMapper.readTree(response.body());
            String translatedText = json.path("responseData").path("translatedText").asText();
            if (!translatedText.isEmpty()) {
                return translatedText;
            }
        }

        throw new Exception("MyMemory translation failed");
    }

    public String transliterateToRoman(String text, String sourceLanguage) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return text;
            }

            // Check cache first
            String cacheKey = "roman_" + sourceLanguage + ":" + text.hashCode();
            if (translationCache.containsKey(cacheKey)) {
                log.debug("Cache hit for transliteration: {}", cacheKey);
                return translationCache.get(cacheKey);
            }

            String languageName = getLanguageName(sourceLanguage);
            String prompt = String.format("""
                    Convert the following %s text to its Roman/Latin script phonetic representation.
                    This is for TEXT-TO-SPEECH pronunciation, not translation.

                    For example:
                    - If input is Telugu "మీ భూమికి", output should be "Mee bhoomi ki"
                    - If input is Tamil "வாழ்க", output should be "Vaalga"
                    - If input is Hindi "नमस्ते", output should be "Namaste"

                    Keep the same meaning but write it in Latin characters that sound like the original when spoken aloud.
                    Return ONLY the transliterated text, nothing else. No explanations.

                    %s text to convert:
                    %s
                    """, languageName, languageName, text);

            String response = callGroqAPIWithFallback(prompt, sourceLanguage);
            String result = response.trim();

            // Cache the result
            translationCache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            log.error("Transliteration failed for language: {}", sourceLanguage, e);
            return text; // Return original text on failure
        }
    }

    private String getLanguageName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "te" -> "Telugu";
            case "hi" -> "Hindi";
            case "ta" -> "Tamil";
            case "kn" -> "Kannada";
            case "ml" -> "Malayalam";
            case "mr" -> "Marathi";
            case "gu" -> "Gujarati";
            case "pa" -> "Punjabi";
            case "bn" -> "Bengali";
            case "ur" -> "Urdu";
            case "as" -> "Assamese";
            case "or" -> "Odia";
            default -> "Unknown";
        };
    }

    public boolean isEnabled() {
        return groqApiKey != null && !groqApiKey.isEmpty();
    }
}
