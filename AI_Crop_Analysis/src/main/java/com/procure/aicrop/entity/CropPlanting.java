package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_plantings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropPlanting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soil_analysis_id")
    private SoilAnalysis soilAnalysis;

    private LocalDate sowingDate;

    private LocalDate expectedHarvestDate;

    @Enumerated(EnumType.STRING)
    private PlantingStatus status;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double areaPlanted;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double estimatedYield;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double currentYieldPrediction;

    @Enumerated(EnumType.STRING)
    private GrowthStage growthStage;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double plantingConfidence;

    @Column(length = 1000)
    private String plantingReason;

    @Column(length = 1000)
    private String riskFactors;

    @Column(columnDefinition = "boolean default false")
    private Boolean hasAlerts;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDate harvestedDate;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double actualYield;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PlantingStatus.PLANNED;
        if (growthStage == null) growthStage = GrowthStage.GERMINATION;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PlantingStatus {
        PLANNED, SOWN, GROWING, MONITORING, HARVESTED, FAILED
    }

    public enum GrowthStage {
        GERMINATION, VEGETATIVE, FLOWERING, GRAIN_FILLING, MATURITY, HARVEST_READY
    }
}
