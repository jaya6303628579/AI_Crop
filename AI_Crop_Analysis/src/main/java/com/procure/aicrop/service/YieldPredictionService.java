package com.procure.aicrop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procure.aicrop.entity.*;
import com.procure.aicrop.repository.YieldPredictionRepository;
import com.procure.aicrop.repository.WeatherDataRepository;
import com.procure.aicrop.repository.CropPlantingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class YieldPredictionService {

    private final YieldPredictionRepository yieldPredictionRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final WeatherDataService weatherDataService;
    private final GroqAIService groqAIService;
    private final CropPlantingRepository cropPlantingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YieldPrediction generateDailyYieldPrediction(CropPlanting cropPlanting, User user) {
        if (cropPlanting.getSowingDate() == null) {
            throw new RuntimeException("Crop has not been sown yet");
        }

        // Make sure today's real weather is on record before we reason over it
        weatherDataService.recordTodayWeather(user);

        int cropAgeInDays = (int) ChronoUnit.DAYS.between(cropPlanting.getSowingDate(), LocalDate.now());
        Integer growingDays = cropPlanting.getCrop().getGrowingDays() != null
                ? cropPlanting.getCrop().getGrowingDays() : 120;

        CropPlanting.GrowthStage growthStage = determineGrowthStage(cropAgeInDays, growingDays);

        String district = user.getDistrict() != null ? user.getDistrict() : "Unknown";
        String state = user.getState() != null ? user.getState() : "Unknown";

        List<WeatherData> recentWeather = weatherDataRepository
                .findByDistrictAndStateAndWeatherDateBetween(
                        district, state, LocalDate.now().minusDays(15), LocalDate.now());

        double avgTemp = recentWeather.stream()
                .mapToDouble(w -> ((w.getMinTemp() != null ? w.getMinTemp() : 0) +
                        (w.getMaxTemp() != null ? w.getMaxTemp() : 0)) / 2.0)
                .average()
                .orElse(28.0);

        double totalRainfall = recentWeather.stream()
                .mapToDouble(w -> w.getRainfall() != null ? w.getRainfall() : 0)
                .sum();

        double avgHumidity = recentWeather.stream()
                .mapToDouble(w -> w.getHumidity() != null ? w.getHumidity() : 0)
                .average()
                .orElse(60.0);

        Double baseYield = cropPlanting.getEstimatedYield() != null
                ? cropPlanting.getEstimatedYield()
                : (cropPlanting.getCrop().getAverageYield() != null ? cropPlanting.getCrop().getAverageYield() : 40.0);

        String aiResponse = groqAIService.predictYield(
                cropPlanting.getCrop().getName(),
                cropAgeInDays,
                growthStage.toString(),
                avgTemp,
                totalRainfall,
                avgHumidity,
                baseYield,
                growingDays
        );

        log.info("AI yield prediction response: {}", aiResponse);

        YieldPrediction prediction = parsePredictionResponse(aiResponse, cropPlanting, cropAgeInDays, growthStage, baseYield);

        if (growthStage == CropPlanting.GrowthStage.MATURITY || growthStage == CropPlanting.GrowthStage.HARVEST_READY) {
            applyHarvestWindowPrediction(prediction, cropPlanting, avgTemp, totalRainfall, district, state);
        }

        YieldPrediction saved = yieldPredictionRepository.save(prediction);

        // Keep the planting's cached fields in sync so dashboard/planting pages reflect the latest AI reading
        cropPlanting.setCurrentYieldPrediction(saved.getPredictedYield());
        cropPlanting.setPlantingConfidence(saved.getConfidence());
        cropPlanting.setGrowthStage(growthStage);
        cropPlanting.setRiskFactors(saved.getRiskAssessment());
        if (cropPlanting.getStatus() == CropPlanting.PlantingStatus.SOWN) {
            cropPlanting.setStatus(CropPlanting.PlantingStatus.MONITORING);
        }
        cropPlantingRepository.save(cropPlanting);

        return saved;
    }

    private YieldPrediction parsePredictionResponse(String aiResponse, CropPlanting cropPlanting,
                                                      int cropAgeInDays, CropPlanting.GrowthStage growthStage,
                                                      Double baseYield) {
        try {
            JsonNode json = objectMapper.readTree(aiResponse);

            Double predictedYield = (json.has("predicted_yield") && !json.get("predicted_yield").isNull())
                    ? json.get("predicted_yield").asDouble() : baseYield;
            Double confidence = json.has("confidence_percentage") ? json.get("confidence_percentage").asDouble() : 70.0;
            Double temperatureImpact = json.has("temperature_impact_multiplier")
                    ? json.get("temperature_impact_multiplier").asDouble() : 1.0;
            Double rainfallImpact = json.has("rainfall_impact_multiplier")
                    ? json.get("rainfall_impact_multiplier").asDouble() : 1.0;
            Double diseaseRisk = json.has("disease_risk_percentage") ? json.get("disease_risk_percentage").asDouble() : 20.0;
            Double pestRisk = json.has("pest_risk_percentage") ? json.get("pest_risk_percentage").asDouble() : 20.0;
            String riskSummary = json.has("risk_summary") ? json.get("risk_summary").asText() : "No specific risks identified.";

            StringBuilder factors = new StringBuilder(riskSummary);
            if (json.has("recommendations") && json.get("recommendations").isArray()) {
                json.get("recommendations").forEach(rec -> factors.append(" ").append(rec.asText()));
            }

            return YieldPrediction.builder()
                    .cropPlanting(cropPlanting)
                    .predictedYield(predictedYield)
                    .confidence(confidence)
                    .cropAgeInDays(cropAgeInDays)
                    .growthStage(growthStage)
                    .factors(factors.toString())
                    .temperatureImpact(temperatureImpact)
                    .rainfallImpact(rainfallImpact)
                    .diseaseRisk(diseaseRisk)
                    .pestRisk(pestRisk)
                    .riskAssessment(riskSummary)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI yield prediction response, using baseline estimate", e);
            return YieldPrediction.builder()
                    .cropPlanting(cropPlanting)
                    .predictedYield(baseYield)
                    .confidence(60.0)
                    .cropAgeInDays(cropAgeInDays)
                    .growthStage(growthStage)
                    .factors("AI prediction unavailable; showing baseline estimate.")
                    .temperatureImpact(1.0)
                    .rainfallImpact(1.0)
                    .diseaseRisk(20.0)
                    .pestRisk(20.0)
                    .riskAssessment("AI prediction unavailable; showing baseline estimate.")
                    .build();
        }
    }

    private void applyHarvestWindowPrediction(YieldPrediction prediction, CropPlanting cropPlanting,
                                                double avgTemp, double totalRainfall, String district, String state) {
        try {
            String region = String.format("%s, %s", district, state);
            String aiResponse = groqAIService.predictHarvestWindow(
                    cropPlanting.getCrop().getName(),
                    cropPlanting.getExpectedHarvestDate(),
                    avgTemp,
                    totalRainfall,
                    region
            );

            log.info("AI harvest window response: {}", aiResponse);

            JsonNode json = objectMapper.readTree(aiResponse);
            LocalDate windowStart = json.has("harvest_window_start")
                    ? parseDateSafe(json.get("harvest_window_start").asText(null)) : null;
            LocalDate windowEnd = json.has("harvest_window_end")
                    ? parseDateSafe(json.get("harvest_window_end").asText(null)) : null;
            String harvestRisk = json.has("harvest_risk") ? json.get("harvest_risk").asText() : "MEDIUM";
            Double rainRisk = json.has("rain_risk_percentage") ? json.get("rain_risk_percentage").asDouble() : 20.0;
            String recommendation = json.has("recommendation") ? json.get("recommendation").asText()
                    : "Monitor weather closely as harvest approaches.";

            prediction.setHarvestWindowStart(windowStart);
            prediction.setHarvestWindowEnd(windowEnd);
            prediction.setHarvestRiskLevel(harvestRisk);
            prediction.setHarvestRainRiskPercentage(rainRisk);
            prediction.setHarvestRecommendation(recommendation);
        } catch (Exception e) {
            log.error("Failed to parse AI harvest window response", e);
        }
    }

    private LocalDate parseDateSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private CropPlanting.GrowthStage determineGrowthStage(int cropAge, Integer totalGrowingDays) {
        if (totalGrowingDays == null) totalGrowingDays = 120;

        int percentage = (cropAge * 100) / totalGrowingDays;

        if (percentage < 15) return CropPlanting.GrowthStage.GERMINATION;
        if (percentage < 35) return CropPlanting.GrowthStage.VEGETATIVE;
        if (percentage < 60) return CropPlanting.GrowthStage.FLOWERING;
        if (percentage < 85) return CropPlanting.GrowthStage.GRAIN_FILLING;
        if (percentage < 95) return CropPlanting.GrowthStage.MATURITY;
        return CropPlanting.GrowthStage.HARVEST_READY;
    }
}
