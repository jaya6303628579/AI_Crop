package com.procure.aicrop.scheduler;

import com.procure.aicrop.entity.Alert;
import com.procure.aicrop.entity.CropPlanting;
import com.procure.aicrop.entity.YieldPrediction;
import com.procure.aicrop.repository.CropPlantingRepository;
import com.procure.aicrop.service.AlertService;
import com.procure.aicrop.service.YieldPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implements the "Daily Crop Monitoring" phase of the crop lifecycle: for every planting
 * that has been sown but not yet harvested, re-fetch live weather, ask the AI to
 * recalculate yield/risk, and raise alerts when risk crosses a threshold - matching the
 * "Weather Changes? -> Recalculate Crop Risk -> Notify Farmer" flow from the product spec.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CropMonitoringScheduler {

    private final CropPlantingRepository cropPlantingRepository;
    private final YieldPredictionService yieldPredictionService;
    private final AlertService alertService;

    private static final List<CropPlanting.PlantingStatus> ACTIVE_STATUSES = List.of(
            CropPlanting.PlantingStatus.SOWN,
            CropPlanting.PlantingStatus.MONITORING
    );

    private static final double HIGH_RISK_THRESHOLD = 40.0;
    private static final double IMPACT_WARNING_THRESHOLD = 0.85;

    @Scheduled(cron = "0 0 6 * * *")
    public void runDailyMonitoring() {
        log.info("Starting daily crop monitoring run");
        List<CropPlanting> activePlantings = cropPlantingRepository.findByStatusIn(ACTIVE_STATUSES);
        log.info("Found {} active plantings to monitor", activePlantings.size());

        for (CropPlanting planting : activePlantings) {
            try {
                monitorPlanting(planting);
            } catch (Exception e) {
                log.error("Failed to monitor planting {}", planting.getId(), e);
            }
        }

        log.info("Daily crop monitoring run complete");
    }

    public void monitorPlanting(CropPlanting planting) {
        YieldPrediction prediction = yieldPredictionService.generateDailyYieldPrediction(planting, planting.getUser());

        if (prediction.getDiseaseRisk() != null && prediction.getDiseaseRisk() >= HIGH_RISK_THRESHOLD) {
            alertService.createAlertIfNotDuplicateToday(
                    planting.getUser(), planting,
                    Alert.AlertType.DISEASE, Alert.AlertSeverity.HIGH,
                    "Elevated disease risk detected",
                    String.format("AI monitoring detected a %.0f%% disease risk for your %s crop (%s stage). %s",
                            prediction.getDiseaseRisk(), planting.getCrop().getName(), prediction.getGrowthStage(),
                            prediction.getRiskAssessment()),
                    "Inspect the field for early disease symptoms and consider preventive treatment.",
                    prediction.getDiseaseRisk());
        }

        if (prediction.getPestRisk() != null && prediction.getPestRisk() >= HIGH_RISK_THRESHOLD) {
            alertService.createAlertIfNotDuplicateToday(
                    planting.getUser(), planting,
                    Alert.AlertType.PEST, Alert.AlertSeverity.HIGH,
                    "Elevated pest risk detected",
                    String.format("AI monitoring detected a %.0f%% pest risk for your %s crop (%s stage). %s",
                            prediction.getPestRisk(), planting.getCrop().getName(), prediction.getGrowthStage(),
                            prediction.getRiskAssessment()),
                    "Monitor closely for pest activity and consult local agronomy guidance if it worsens.",
                    prediction.getPestRisk());
        }

        boolean weatherUnfavorable = (prediction.getTemperatureImpact() != null && prediction.getTemperatureImpact() < IMPACT_WARNING_THRESHOLD)
                || (prediction.getRainfallImpact() != null && prediction.getRainfallImpact() < IMPACT_WARNING_THRESHOLD);

        if (weatherUnfavorable) {
            Double combinedRisk = 100.0 - (Math.min(
                    prediction.getTemperatureImpact() != null ? prediction.getTemperatureImpact() : 1.0,
                    prediction.getRainfallImpact() != null ? prediction.getRainfallImpact() : 1.0) * 100.0);

            alertService.createAlertIfNotDuplicateToday(
                    planting.getUser(), planting,
                    Alert.AlertType.WEATHER, Alert.AlertSeverity.MEDIUM,
                    "Weather conditions may affect your crop",
                    String.format("Recent weather is not ideal for your %s crop at the %s stage. %s",
                            planting.getCrop().getName(), prediction.getGrowthStage(), prediction.getRiskAssessment()),
                    "Review irrigation/drainage and monitor crop health closely over the next few days.",
                    combinedRisk);
        }

        if (planting.getGrowthStage() == CropPlanting.GrowthStage.HARVEST_READY
                || planting.getGrowthStage() == CropPlanting.GrowthStage.MATURITY) {
            boolean hasWindow = prediction.getHarvestWindowStart() != null && prediction.getHarvestWindowEnd() != null;
            String message = hasWindow
                    ? String.format("Your %s crop is nearing harvest. AI-predicted harvest window: %s to %s (risk: %s).",
                            planting.getCrop().getName(), prediction.getHarvestWindowStart(),
                            prediction.getHarvestWindowEnd(), prediction.getHarvestRiskLevel())
                    : String.format("Your %s crop has reached the %s stage.", planting.getCrop().getName(), planting.getGrowthStage());

            alertService.createAlertIfNotDuplicateToday(
                    planting.getUser(), planting,
                    Alert.AlertType.HARVEST, Alert.AlertSeverity.LOW,
                    "Harvest window update",
                    message,
                    hasWindow ? prediction.getHarvestRecommendation() : "Plan harvesting during a dry weather window for best results.",
                    prediction.getHarvestRainRiskPercentage() != null ? prediction.getHarvestRainRiskPercentage() : 0.0);
        }
    }
}
