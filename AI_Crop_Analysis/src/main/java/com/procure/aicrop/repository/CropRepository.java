package com.procure.aicrop.repository;

import com.procure.aicrop.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {
    Optional<Crop> findByName(String name);
    Optional<Crop> findByNameIgnoreCase(String name);
    List<Crop> findAll();
}
