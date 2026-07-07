package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "crops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private Integer growingDays;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double waterRequirement;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double minTemperature;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double maxTemperature;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double optimalTemperature;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double minRainfall;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double maxRainfall;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> suitableSoilTypes;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> sowingMonths;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double averageYield;

    private String imageUrl;

    @Column(length = 500)
    private String remarks;

    /** Local/regional-language name (e.g. Telugu), shown alongside the English name so
     *  farmers can recognize crops by the name they actually use. */
    private String localName;
}
