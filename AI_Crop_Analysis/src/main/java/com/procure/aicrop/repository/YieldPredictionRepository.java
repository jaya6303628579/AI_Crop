package com.procure.aicrop.repository;

import com.procure.aicrop.entity.YieldPrediction;
import com.procure.aicrop.entity.CropPlanting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface YieldPredictionRepository extends JpaRepository<YieldPrediction, Long> {
    List<YieldPrediction> findByCropPlantingOrderByPredictedAtDesc(CropPlanting cropPlanting);
    Optional<YieldPrediction> findFirstByCropPlantingOrderByPredictedAtDesc(CropPlanting cropPlanting);
}
