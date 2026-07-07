package com.procure.aicrop.repository;

import com.procure.aicrop.entity.Alert;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.entity.CropPlanting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserOrderByAlertTimeDesc(User user);
    List<Alert> findByUserAndIsReadFalseOrderByAlertTimeDesc(User user);
    List<Alert> findByCropPlantingOrderByAlertTimeDesc(CropPlanting cropPlanting);
    List<Alert> findByUserAndSeverityOrderByAlertTimeDesc(User user, Alert.AlertSeverity severity);
    List<Alert> findByCropPlantingAndTypeAndCreatedAtAfter(
            CropPlanting cropPlanting, Alert.AlertType type, LocalDateTime after);
}
