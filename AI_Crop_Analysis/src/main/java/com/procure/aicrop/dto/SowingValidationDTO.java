package com.procure.aicrop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowingValidationDTO {
    private String cropName;
    private LocalDate plannedDate;
    private Boolean suitable;
    private String verdict;
    private Double survivalProbability;
    private String reasoning;
    private List<String> riskFactors;
    private LocalDate recommendedSowingStart;
    private LocalDate recommendedSowingEnd;
    private Double currentTemperature;
    private Double forecastRainfall;
    private String weatherCondition;
    private Integer daysUntilPlanting;
    private Boolean forecastBased;
    private List<String> typicalSowingMonths;
}
