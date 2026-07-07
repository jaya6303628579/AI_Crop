package com.procure.aicrop.service;

import com.procure.aicrop.dto.AlertDTO;
import com.procure.aicrop.entity.*;
import com.procure.aicrop.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final com.procure.aicrop.repository.CropPlantingRepository cropPlantingRepository;

    /**
     * Creates an alert only if one of the same type hasn't already been created for this
     * planting today - prevents the daily monitoring job from spamming duplicate alerts.
     */
    public AlertDTO createAlertIfNotDuplicateToday(
            User user,
            CropPlanting cropPlanting,
            Alert.AlertType type,
            Alert.AlertSeverity severity,
            String title,
            String message,
            String recommendation,
            Double riskPercentage) {

        if (cropPlanting != null) {
            List<Alert> todayAlerts = alertRepository.findByCropPlantingAndTypeAndCreatedAtAfter(
                    cropPlanting, type, LocalDate.now().atStartOfDay());
            if (!todayAlerts.isEmpty()) {
                return convertToDTO(todayAlerts.get(0));
            }
        }

        return createAlert(user, cropPlanting, type, severity, title, message, recommendation, riskPercentage);
    }

    public AlertDTO createAlert(
            User user,
            CropPlanting cropPlanting,
            Alert.AlertType type,
            Alert.AlertSeverity severity,
            String title,
            String message,
            String recommendation,
            Double riskPercentage) {

        Alert alert = Alert.builder()
                .user(user)
                .cropPlanting(cropPlanting)
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .recommendation(recommendation)
                .riskPercentage(riskPercentage)
                .build();

        // Mark crop as having alerts
        if (cropPlanting != null) {
            cropPlanting.setHasAlerts(true);
            cropPlantingRepository.save(cropPlanting);
        }

        return convertToDTO(alertRepository.save(alert));
    }

    public List<AlertDTO> getUserAlerts(User user) {
        return alertRepository.findByUserOrderByAlertTimeDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getUnreadAlerts(User user) {
        return alertRepository.findByUserAndIsReadFalseOrderByAlertTimeDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getCriticalAlerts(User user) {
        return alertRepository.findByUserAndSeverityOrderByAlertTimeDesc(
                user, Alert.AlertSeverity.CRITICAL)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getAlertsByCropPlanting(CropPlanting cropPlanting) {
        return alertRepository.findByCropPlantingOrderByAlertTimeDesc(cropPlanting)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long alertId, User user) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            if (alert.getUser().getId().equals(user.getId())) {
                alert.setIsRead(true);
                alertRepository.save(alert);
            }
        });
    }

    public void markAllAsRead(User user) {
        List<Alert> unreadAlerts = alertRepository.findByUserAndIsReadFalseOrderByAlertTimeDesc(user);
        unreadAlerts.forEach(alert -> alert.setIsRead(true));
        alertRepository.saveAll(unreadAlerts);
    }

    public void deleteAlert(Long alertId, User user) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            if (alert.getUser().getId().equals(user.getId())) {
                alertRepository.delete(alert);
            }
        });
    }

    public AlertDTO convertToDTO(Alert alert) {
        return AlertDTO.fromEntity(alert);
    }
}
