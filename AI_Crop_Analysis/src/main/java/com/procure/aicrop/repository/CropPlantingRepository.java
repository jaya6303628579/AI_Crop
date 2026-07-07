package com.procure.aicrop.repository;

import com.procure.aicrop.entity.CropPlanting;
import com.procure.aicrop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CropPlantingRepository extends JpaRepository<CropPlanting, Long> {
    List<CropPlanting> findByUser(User user);
    List<CropPlanting> findByUserAndStatus(User user, CropPlanting.PlantingStatus status);
    List<CropPlanting> findByUserOrderByCreatedAtDesc(User user);
    Optional<CropPlanting> findByIdAndUser(Long id, User user);
    List<CropPlanting> findByStatusIn(List<CropPlanting.PlantingStatus> statuses);
}
