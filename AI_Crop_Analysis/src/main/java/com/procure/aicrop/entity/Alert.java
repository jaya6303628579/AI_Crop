package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_planting_id")
    private CropPlanting cropPlanting;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 1000)
    private String recommendation;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double riskPercentage;

    @Column(columnDefinition = "boolean default false")
    private Boolean isRead;

    private LocalDateTime alertTime;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        alertTime = LocalDateTime.now();
        isRead = false;
    }

    public enum AlertType {
        WEATHER, DISEASE, PEST, SOIL, GROWTH, HARVEST, CRITICAL
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
