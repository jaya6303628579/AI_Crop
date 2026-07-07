package com.procure.aicrop.repository;

import com.procure.aicrop.entity.SoilAnalysis;
import com.procure.aicrop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SoilAnalysisRepository extends JpaRepository<SoilAnalysis, Long> {
    List<SoilAnalysis> findByUserOrderByAnalyzedAtDesc(User user);
    Optional<SoilAnalysis> findFirstByUserOrderByAnalyzedAtDesc(User user);
    Optional<SoilAnalysis> findByIdAndUser(Long id, User user);
}
