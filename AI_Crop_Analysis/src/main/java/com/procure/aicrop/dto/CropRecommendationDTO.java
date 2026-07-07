package com.procure.aicrop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropRecommendationDTO {
    private Long recommendationId;
    private Long cropId;
    private String cropName;
    private LocalDate sowingStartDate;
    private LocalDate sowingEndDate;
    private Double confidence;
    private String reasoning;
    private String benefitExplanation;
    private String dangerousDatesExplanation;
    private LocalDate avoidSowingStart;
    private LocalDate avoidSowingEnd;
    private Double expectedRainfallMm;
    private String dangerRiskLevel;
    private Double expectedYield;
    private String imageUrl;
    private String localName;
}
