package com.procure.aicrop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phoneNumber;

    private String state;

    private String district;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double latitude;

    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double longitude;

    @Column(length = 1000)
    private String address;

    private Long areaAcres;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(columnDefinition = "boolean default true")
    private Boolean active;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserRole {
        FARMER, ADMIN, AGRONOMIST
    }
}
