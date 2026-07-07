package com.procure.aicrop.repository;

import com.procure.aicrop.entity.Recommendation;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserOrderByCreatedAtDesc(User user);
    List<Recommendation> findByUserAndIsAcceptedFalseOrderByCreatedAtDesc(User user);
    List<Recommendation> findByUserAndCropOrderByCreatedAtDesc(User user, Crop crop);
}
