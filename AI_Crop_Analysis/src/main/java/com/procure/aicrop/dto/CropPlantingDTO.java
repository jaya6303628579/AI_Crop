package com.procure.aicrop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.procure.aicrop.entity.CropPlanting;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropPlantingDTO {
    private Long id;
    private Long cropId;
    private String cropName;
    private String cropIcon;
    private LocalDate sowingDate;
    private LocalDate expectedHarvestDate;
    private CropPlanting.PlantingStatus status;
    private Double areaPlanted;
    private Double estimatedYield;
    private Double currentYieldPrediction;
    private CropPlanting.GrowthStage growthStage;
    private Double plantingConfidence;
    private String plantingReason;
    private String riskFactors;
    private Boolean hasAlerts;
    private Integer cropAgeInDays;

    public static CropPlantingDTO fromEntity(CropPlanting planting) {
        int cropAge = 0;
        if (planting.getSowingDate() != null) {
            cropAge = (int) java.time.temporal.ChronoUnit.DAYS.between(
                    planting.getSowingDate(),
                    LocalDate.now()
            );
        }

        return CropPlantingDTO.builder()
                .id(planting.getId())
                .cropId(planting.getCrop().getId())
                .cropName(planting.getCrop().getName())
                .cropIcon(planting.getCrop().getImageUrl())
                .sowingDate(planting.getSowingDate())
                .expectedHarvestDate(planting.getExpectedHarvestDate())
                .status(planting.getStatus())
                .areaPlanted(planting.getAreaPlanted())
                .estimatedYield(planting.getEstimatedYield())
                .currentYieldPrediction(planting.getCurrentYieldPrediction())
                .growthStage(planting.getGrowthStage())
                .plantingConfidence(planting.getPlantingConfidence())
                .plantingReason(planting.getPlantingReason())
                .riskFactors(planting.getRiskFactors())
                .hasAlerts(planting.getHasAlerts())
                .cropAgeInDays(cropAge)
                .build();
    }
}
