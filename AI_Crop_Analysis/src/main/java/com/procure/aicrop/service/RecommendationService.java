package com.procure.aicrop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procure.aicrop.dto.CropRecommendationDTO;
import com.procure.aicrop.entity.*;
import com.procure.aicrop.repository.RecommendationRepository;
import com.procure.aicrop.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final CropService cropService;
    private final GroqAIService groqAIService;
    private final WeatherAPIService weatherAPIService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CropRecommendationDTO generateCropSuitabilityRecommendation(
            User user,
            Crop crop,
            SoilAnalysis soilAnalysis) {

        // Check soil suitability
        String soilTypeName = soilAnalysis.getSoilType().toString();
        boolean isSoilSuitable = crop.getSuitableSoilTypes() != null &&
                crop.getSuitableSoilTypes().contains(soilTypeName);

        String reasoning = buildSuitabilityReasoning(crop, soilAnalysis, isSoilSuitable);

        // Get historical weather data for the district
        List<WeatherData> historicalWeather = weatherDataRepository
                .findByDistrictAndStateOrderByWeatherDateDesc(user.getDistrict(), user.getState());

        // Calculate best sowing window
        LocalDate now = LocalDate.now();
        LocalDate sowingStart = calculateOptimalSowingStart(crop, now, historicalWeather);
        LocalDate sowingEnd = sowingStart.plusDays(7);

        Double confidence = calculateConfidence(isSoilSuitable, soilAnalysis.getConfidence());
        Double expectedYield = crop.getAverageYield() * (confidence / 100.0);

        Recommendation recommendation = Recommendation.builder()
                .user(user)
                .crop(crop)
                .soilAnalysis(soilAnalysis)
                .type(Recommendation.RecommendationType.CROP_SUITABILITY)
                .sowingStartDate(sowingStart)
                .sowingEndDate(sowingEnd)
                .confidence(confidence)
                .reasoning(reasoning)
                .benefitExplanation(buildBenefitExplanation(crop, sowingStart))
                .dangerousDatesExplanation(buildDangerousDatesExplanation(crop, sowingStart, sowingEnd))
                .expectedYield(expectedYield)
                .build();

        return convertToDTO(recommendationRepository.save(recommendation));
    }

    /**
     * Validates whether a farmer's chosen crop + planned sowing date is safe, using the
     * farmer's LIVE device location (not their stored profile location) for real weather,
     * and recommends a corrected sowing window if the plan is risky or off-season.
     */
    public com.procure.aicrop.dto.SowingValidationDTO validateSowingPlan(
            Crop crop, LocalDate plannedDate, Double latitude, Double longitude, User user) {

        Double lat = latitude != null ? latitude : 17.3850;
        Double lon = longitude != null ? longitude : 78.4867;

        LocalDate today = LocalDate.now();
        long daysUntilPlanting = java.time.temporal.ChronoUnit.DAYS.between(today, plannedDate);

        Double temperature;
        Double rainfall;
        String condition;
        boolean forecastBased;

        if (daysUntilPlanting >= 0 && daysUntilPlanting <= 5) {
            // Planned date is within OpenWeatherMap's real forecast horizon - use that
            // specific day's actual forecast instead of today's weather.
            Map<String, Object> dateForecast = weatherAPIService.getForecastForDate(lat, lon, plannedDate);
            if (Boolean.TRUE.equals(dateForecast.get("available"))) {
                temperature = toDouble(dateForecast.get("temperature"), 28.0);
                rainfall = toDouble(dateForecast.get("rainfall"), 0.0);
                condition = dateForecast.get("condition") != null ? dateForecast.get("condition").toString() : "Unknown";
                forecastBased = true;
            } else {
                Map<String, Object> currentWeather = weatherAPIService.getCurrentWeather(lat, lon);
                temperature = toDouble(currentWeather.get("temperature"), 28.0);
                rainfall = 0.0;
                condition = currentWeather.get("condition") != null ? currentWeather.get("condition").toString() : "Unknown";
                forecastBased = false;
            }
        } else if (daysUntilPlanting < 0) {
            // Date in the past (shouldn't normally happen, but stay honest about it)
            Map<String, Object> currentWeather = weatherAPIService.getCurrentWeather(lat, lon);
            temperature = toDouble(currentWeather.get("temperature"), 28.0);
            rainfall = 0.0;
            condition = currentWeather.get("condition") != null ? currentWeather.get("condition").toString() : "Unknown";
            forecastBased = false;
        } else {
            // Beyond real forecast range - do not pretend today's weather predicts that date.
            // Ground the AI in the crop's own typical sowing months from our catalog instead.
            Map<String, Object> currentWeather = weatherAPIService.getCurrentWeather(lat, lon);
            temperature = toDouble(currentWeather.get("temperature"), 28.0);
            rainfall = 0.0;
            condition = currentWeather.get("condition") != null ? currentWeather.get("condition").toString() : "Unknown";
            forecastBased = false;
        }

        String region = String.format("%s, %s",
                user != null && user.getDistrict() != null ? user.getDistrict() : "Unknown district",
                user != null && user.getState() != null ? user.getState() : "India");

        String typicalSowingMonths = crop.getSowingMonths() != null && !crop.getSowingMonths().isEmpty()
                ? String.join(", ", crop.getSowingMonths()) : "Unknown";

        String aiResponse = groqAIService.validateSowingPlan(
                crop.getName(), plannedDate, temperature, rainfall, region, crop.getGrowingDays(),
                daysUntilPlanting, forecastBased, typicalSowingMonths);

        log.info("AI sowing validation response: {}", aiResponse);

        com.procure.aicrop.dto.SowingValidationDTO.SowingValidationDTOBuilder builder =
                com.procure.aicrop.dto.SowingValidationDTO.builder()
                        .cropName(crop.getName())
                        .plannedDate(plannedDate)
                        .currentTemperature(temperature)
                        .forecastRainfall(rainfall)
                        .weatherCondition(condition)
                        .daysUntilPlanting((int) daysUntilPlanting)
                        .forecastBased(forecastBased)
                        .typicalSowingMonths(crop.getSowingMonths());

        try {
            JsonNode json = objectMapper.readTree(aiResponse);
            boolean suitable = json.path("suitable").asBoolean(true);
            String verdict = json.has("verdict") ? json.get("verdict").asText() : "GOOD";
            Double survivalProbability = json.has("survival_probability") ? json.get("survival_probability").asDouble() : 70.0;
            String reasoning = json.has("reasoning") ? json.get("reasoning").asText() : "No specific concerns identified.";

            List<String> riskFactors = new ArrayList<>();
            if (json.has("risk_factors") && json.get("risk_factors").isArray()) {
                json.get("risk_factors").forEach(r -> riskFactors.add(r.asText()));
            }

            LocalDate recommendedStart = parseDateOrDefault(
                    json.has("recommended_sowing_start") ? json.get("recommended_sowing_start").asText(null) : null,
                    plannedDate);
            LocalDate recommendedEnd = parseDateOrDefault(
                    json.has("recommended_sowing_end") ? json.get("recommended_sowing_end").asText(null) : null,
                    recommendedStart.plusDays(7));

            builder.suitable(suitable)
                    .verdict(verdict)
                    .survivalProbability(survivalProbability)
                    .reasoning(reasoning)
                    .riskFactors(riskFactors)
                    .recommendedSowingStart(recommendedStart)
                    .recommendedSowingEnd(recommendedEnd);
        } catch (Exception e) {
            log.error("Failed to parse AI sowing validation response", e);
            builder.suitable(true)
                    .verdict("GOOD")
                    .survivalProbability(70.0)
                    .reasoning("AI validation unavailable right now; showing a neutral baseline assessment.")
                    .riskFactors(new ArrayList<>())
                    .recommendedSowingStart(plannedDate)
                    .recommendedSowingEnd(plannedDate.plusDays(7));
        }

        return builder.build();
    }

    public List<CropRecommendationDTO> generateAlternativeCropRecommendations(
            User user,
            SoilAnalysis soilAnalysis) {

        Double latitude = user.getLatitude() != null ? user.getLatitude() : 17.3850;
        Double longitude = user.getLongitude() != null ? user.getLongitude() : 78.4867;

        Map<String, Object> currentWeather = weatherAPIService.getCurrentWeather(latitude, longitude);
        Map<String, Object> forecast = weatherAPIService.getWeatherForecast(latitude, longitude, 7);

        Double temperature = toDouble(currentWeather.get("temperature"), 28.0);
        Double rainfall = toDouble(forecast.get("total_rainfall"), 50.0);
        String region = String.format("%s, %s",
                user.getDistrict() != null ? user.getDistrict() : "Unknown district",
                user.getState() != null ? user.getState() : "India");

        String aiResponse = groqAIService.generateCropRecommendation(
                soilAnalysis.getSoilType().toString(),
                soilAnalysis.getPH(),
                soilAnalysis.getNitrogen(),
                soilAnalysis.getPhosphorus(),
                soilAnalysis.getPotassium(),
                rainfall,
                temperature,
                region
        );

        log.info("AI crop recommendation response: {}", aiResponse);

        List<CropRecommendationDTO> results = new ArrayList<>();
        try {
            JsonNode json = objectMapper.readTree(aiResponse);
            String overallReason = json.path("reason").asText("Recommended by AI based on your soil and weather conditions");
            JsonNode recs = json.path("recommendations");

            for (JsonNode rec : recs) {
                String cropName = rec.path("crop_name").asText(null);
                if (cropName == null || cropName.isBlank()) {
                    continue;
                }

                Crop crop = cropService.getCropByNameIgnoreCase(cropName)
                        .orElseGet(() -> cropService.createCrop(Crop.builder()
                                .name(cropName)
                                .description("AI-recommended crop based on soil and weather analysis")
                                .build()));

                LocalDate sowingStart = parseDateOrDefault(rec.path("sowing_start").asText(null), LocalDate.now().plusDays(5));
                LocalDate sowingEnd = parseDateOrDefault(rec.path("sowing_end").asText(null), sowingStart.plusDays(14));
                Double confidence = rec.has("suitability_score") ? rec.get("suitability_score").asDouble() : 75.0;
                Double expectedYield = rec.has("expected_yield") ? rec.get("expected_yield").asDouble()
                        : (crop.getAverageYield() != null ? crop.getAverageYield() : 0.0);
                String benefitExplanation = rec.has("benefit_explanation") ? rec.get("benefit_explanation").asText() : overallReason;
                String riskExplanation = rec.has("risk_explanation") ? rec.get("risk_explanation").asText()
                        : "No specific risk factors identified by AI for this recommendation.";

                JsonNode dangerousDates = rec.path("dangerous_dates");
                LocalDate avoidStart = dangerousDates.has("avoid_start")
                        ? parseDateOrDefault(dangerousDates.get("avoid_start").asText(null), null) : null;
                LocalDate avoidEnd = dangerousDates.has("avoid_end")
                        ? parseDateOrDefault(dangerousDates.get("avoid_end").asText(null), null) : null;
                Double expectedRainfallMm = dangerousDates.has("expected_rainfall_mm")
                        ? dangerousDates.get("expected_rainfall_mm").asDouble() : null;
                String dangerRiskLevel = dangerousDates.has("risk_level")
                        ? dangerousDates.get("risk_level").asText() : null;

                Recommendation recommendation = Recommendation.builder()
                        .user(user)
                        .crop(crop)
                        .soilAnalysis(soilAnalysis)
                        .type(Recommendation.RecommendationType.ALTERNATIVE_CROP)
                        .sowingStartDate(sowingStart)
                        .sowingEndDate(sowingEnd)
                        .confidence(confidence)
                        .reasoning(overallReason)
                        .benefitExplanation(benefitExplanation)
                        .dangerousDatesExplanation(riskExplanation)
                        .avoidSowingStart(avoidStart)
                        .avoidSowingEnd(avoidEnd)
                        .expectedRainfallMm(expectedRainfallMm)
                        .dangerRiskLevel(dangerRiskLevel)
                        .expectedYield(expectedYield)
                        .build();

                results.add(convertToDTO(recommendationRepository.save(recommendation)));
            }
        } catch (Exception e) {
            log.error("Failed to parse AI crop recommendation response", e);
        }

        return results;
    }

    private Double toDouble(Object value, Double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    private LocalDate parseDateOrDefault(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    public void acceptRecommendation(Long recommendationId, User user) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        if (!recommendation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        recommendation.setIsAccepted(true);
        recommendation.setAcceptedDate(LocalDate.now());
        recommendationRepository.save(recommendation);
    }

    public List<CropRecommendationDTO> getUserRecommendations(User user) {
        return recommendationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private LocalDate calculateOptimalSowingStart(Crop crop, LocalDate today, List<WeatherData> weatherData) {
        LocalDate sowingDate = today.plusDays(5);

        // Find a date with favorable rainfall (between min and max)
        for (int i = 0; i < 30; i++) {
            LocalDate checkDate = today.plusDays(i);
            boolean hasGoodWeather = weatherData.stream()
                    .filter(w -> w.getWeatherDate().isEqual(checkDate))
                    .anyMatch(w -> w.getRainfall() != null &&
                            w.getRainfall() >= crop.getMinRainfall() &&
                            w.getRainfall() <= crop.getMaxRainfall());

            if (hasGoodWeather || i > 10) {
                sowingDate = checkDate;
                break;
            }
        }

        return sowingDate;
    }

    private String buildSuitabilityReasoning(Crop crop, SoilAnalysis soil, boolean isSuitable) {
        StringBuilder reasoning = new StringBuilder();

        if (isSuitable) {
            reasoning.append("This crop is well-suited for your soil type. ");
        } else {
            reasoning.append("Soil type may be suboptimal for this crop. ");
        }

        if (soil.getOrganicMatter() != null && soil.getOrganicMatter() > 2.0) {
            reasoning.append("Your soil has good organic matter content. ");
        }

        if (soil.getPH() != null && soil.getPH() >= 6.0 && soil.getPH() <= 8.0) {
            reasoning.append("Soil pH is favorable. ");
        }

        return reasoning.toString();
    }

    private String buildBenefitExplanation(Crop crop, LocalDate sowingDate) {
        return String.format(
                "Sowing %s on or around %s will provide: optimal soil moisture conditions after seasonal rainfall, " +
                "favorable temperature range of %s-%.1f°C for germination and early growth, and alignment with " +
                "traditional sowing practices in your region.",
                crop.getName(), sowingDate, crop.getMinTemperature(), crop.getMaxTemperature()
        );
    }

    private String buildDangerousDatesExplanation(Crop crop, LocalDate sowingStart, LocalDate sowingEnd) {
        return String.format(
                "Avoid sowing 1-7 days before %s due to predicted water logging. Avoid sowing after %s " +
                "as seedlings may face stress from temperature fluctuations.",
                sowingStart, sowingEnd.plusDays(7)
        );
    }

    private Double calculateConfidence(boolean isSoilSuitable, Double soilConfidence) {
        double baseConfidence = isSoilSuitable ? 85.0 : 60.0;
        return Math.min(99.0, baseConfidence * (soilConfidence / 100.0));
    }

    private CropRecommendationDTO convertToDTO(Recommendation recommendation) {
        return CropRecommendationDTO.builder()
                .recommendationId(recommendation.getId())
                .cropId(recommendation.getCrop().getId())
                .cropName(recommendation.getCrop().getName())
                .sowingStartDate(recommendation.getSowingStartDate())
                .sowingEndDate(recommendation.getSowingEndDate())
                .confidence(recommendation.getConfidence())
                .reasoning(recommendation.getReasoning())
                .benefitExplanation(recommendation.getBenefitExplanation())
                .dangerousDatesExplanation(recommendation.getDangerousDatesExplanation())
                .avoidSowingStart(recommendation.getAvoidSowingStart())
                .avoidSowingEnd(recommendation.getAvoidSowingEnd())
                .expectedRainfallMm(recommendation.getExpectedRainfallMm())
                .dangerRiskLevel(recommendation.getDangerRiskLevel())
                .expectedYield(recommendation.getExpectedYield())
                .imageUrl(recommendation.getCrop().getImageUrl())
                .localName(recommendation.getCrop().getLocalName())
                .build();
    }
}
