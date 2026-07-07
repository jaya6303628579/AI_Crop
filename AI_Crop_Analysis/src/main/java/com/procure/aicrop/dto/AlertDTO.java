package com.procure.aicrop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.procure.aicrop.entity.Alert;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDTO {
    private Long id;
    private Alert.AlertType type;
    private Alert.AlertSeverity severity;
    private String title;
    private String message;
    private String recommendation;
    private Double riskPercentage;
    private Boolean isRead;
    private LocalDateTime alertTime;
    private Long cropPlantingId;

    public static AlertDTO fromEntity(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .recommendation(alert.getRecommendation())
                .riskPercentage(alert.getRiskPercentage())
                .isRead(alert.getIsRead())
                .alertTime(alert.getAlertTime())
                .cropPlantingId(alert.getCropPlanting() != null ? alert.getCropPlanting().getId() : null)
                .build();
    }
}
