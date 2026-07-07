package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "soil_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private SoilType soilType;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double pH;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double nitrogen;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double phosphorus;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double potassium;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double organicMatter;

    @Enumerated(EnumType.STRING)
    private SoilTexture texture;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double moisture;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double confidence;

    @Column(length = 1000)
    private String analysis;

    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }

    public enum SoilType {
        CLAY, LOAMY, SANDY, SILTY, PEAT, LATERITE
    }

    public enum SoilTexture {
        FINE, MEDIUM, COARSE
    }
}
