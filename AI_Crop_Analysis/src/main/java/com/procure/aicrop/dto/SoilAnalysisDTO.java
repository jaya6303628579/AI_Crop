package com.procure.aicrop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.procure.aicrop.entity.SoilAnalysis;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilAnalysisDTO {
    private Long id;
    private String imageUrl;
    private SoilAnalysis.SoilType soilType;
    @JsonProperty("pH")
    private Double pH;
    private Double nitrogen;
    private Double phosphorus;
    private Double potassium;
    private Double organicMatter;
    private SoilAnalysis.SoilTexture texture;
    private Double moisture;
    private Double confidence;
    private String analysis;

    public static SoilAnalysisDTO fromEntity(SoilAnalysis soil) {
        return SoilAnalysisDTO.builder()
                .id(soil.getId())
                .imageUrl(soil.getImageUrl())
                .soilType(soil.getSoilType())
                .pH(soil.getPH())
                .nitrogen(soil.getNitrogen())
                .phosphorus(soil.getPhosphorus())
                .potassium(soil.getPotassium())
                .organicMatter(soil.getOrganicMatter())
                .texture(soil.getTexture())
                .moisture(soil.getMoisture())
                .confidence(soil.getConfidence())
                .analysis(soil.getAnalysis())
                .build();
    }
}
