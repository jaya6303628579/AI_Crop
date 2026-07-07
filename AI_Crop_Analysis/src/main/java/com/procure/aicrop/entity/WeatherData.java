package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String district;

    private String state;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double latitude;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double longitude;

    private LocalDate weatherDate;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double minTemp;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double maxTemp;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double rainfall;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double humidity;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double windSpeed;

    private String weatherCondition;

    @Column(columnDefinition = "boolean default false")
    private Boolean isForecast;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double confidence;

    private LocalDateTime fetchedAt;

    @PrePersist
    protected void onCreate() {
        fetchedAt = LocalDateTime.now();
    }
}
