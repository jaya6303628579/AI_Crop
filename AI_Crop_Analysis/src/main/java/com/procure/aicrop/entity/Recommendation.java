package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {
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

    @Enumerated(EnumType.STRING)
    private RecommendationType type;

    private LocalDate sowingStartDate;

    private LocalDate sowingEndDate;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double confidence;

    @Column(length = 1000)
    private String reasoning;

    @Column(length = 1000)
    private String benefitExplanation;

    @Column(columnDefinition = "TEXT")
    private String dangerousDatesExplanation;

    private LocalDate avoidSowingStart;

    private LocalDate avoidSowingEnd;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double expectedRainfallMm;

    private String dangerRiskLevel;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double expectedYield;

    @Column(columnDefinition = "boolean default false")
    private Boolean isAccepted;

    private LocalDate acceptedDate;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RecommendationType {
        CROP_SUITABILITY, SOWING_WINDOW, HARVEST_WINDOW, ALTERNATIVE_CROP
    }
}
