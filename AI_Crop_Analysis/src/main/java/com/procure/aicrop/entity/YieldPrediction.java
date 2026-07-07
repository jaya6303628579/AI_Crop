package com.procure.aicrop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "yield_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YieldPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_planting_id", nullable = false)
    private CropPlanting cropPlanting;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double predictedYield;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double confidence;

    private Integer cropAgeInDays;

    @Enumerated(EnumType.STRING)
    private CropPlanting.GrowthStage growthStage;

    @Column(length = 1000)
    private String factors;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double temperatureImpact;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double rainfallImpact;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double diseaseRisk;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double pestRisk;

    @Column(length = 1000)
    private String riskAssessment;

    private LocalDate harvestWindowStart;

    private LocalDate harvestWindowEnd;

    private String harvestRiskLevel;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double harvestRainRiskPercentage;

    @Column(length = 500)
    private String harvestRecommendation;

    private LocalDateTime predictedAt;

    @PrePersist
    protected void onCreate() {
        predictedAt = LocalDateTime.now();
    }
}
