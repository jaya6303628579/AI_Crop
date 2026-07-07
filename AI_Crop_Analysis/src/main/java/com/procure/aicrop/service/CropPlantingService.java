package com.procure.aicrop.service;

import com.procure.aicrop.dto.CropPlantingDTO;
import com.procure.aicrop.entity.*;
import com.procure.aicrop.repository.CropPlantingRepository;
import com.procure.aicrop.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CropPlantingService {

    private final CropPlantingRepository cropPlantingRepository;
    private final CropRepository cropRepository;

    public CropPlantingDTO createCropPlanting(
            User user,
            Long cropId,
            LocalDate sowingDate,
            Double areaPlanted,
            SoilAnalysis soilAnalysis,
            Recommendation recommendation) {

        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new RuntimeException("Crop not found"));

        LocalDate expectedHarvestDate = sowingDate.plusDays(crop.getGrowingDays());

        CropPlanting planting = CropPlanting.builder()
                .user(user)
                .crop(crop)
                .soilAnalysis(soilAnalysis)
                .sowingDate(sowingDate)
                .expectedHarvestDate(expectedHarvestDate)
                .status(CropPlanting.PlantingStatus.SOWN)
                .areaPlanted(areaPlanted)
                .estimatedYield(recommendation != null ? recommendation.getExpectedYield() : crop.getAverageYield())
                .currentYieldPrediction(recommendation != null ? recommendation.getExpectedYield() : crop.getAverageYield())
                .growthStage(CropPlanting.GrowthStage.GERMINATION)
                .plantingConfidence(recommendation != null ? recommendation.getConfidence() : 75.0)
                .plantingReason(recommendation != null ? recommendation.getReasoning() : "Manual planting")
                .hasAlerts(false)
                .build();

        return convertToDTO(cropPlantingRepository.save(planting));
    }

    public List<CropPlantingDTO> getUserPlantings(User user) {
        return cropPlantingRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CropPlantingDTO getPlanting(Long plantingId, User user) {
        CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                .orElseThrow(() -> new RuntimeException("Planting not found"));

        return convertToDTO(planting);
    }

    public void updateGrowthStage(Long plantingId, CropPlanting.GrowthStage stage, User user) {
        CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                .orElseThrow(() -> new RuntimeException("Planting not found"));

        planting.setGrowthStage(stage);
        cropPlantingRepository.save(planting);
    }

    public void updatePlantingStatus(Long plantingId, CropPlanting.PlantingStatus status, User user) {
        CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                .orElseThrow(() -> new RuntimeException("Planting not found"));

        planting.setStatus(status);

        if (status == CropPlanting.PlantingStatus.HARVESTED) {
            planting.setHarvestedDate(LocalDate.now());
        }

        cropPlantingRepository.save(planting);
    }

    public void updateYieldPrediction(Long plantingId, Double yieldPrediction, User user) {
        CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                .orElseThrow(() -> new RuntimeException("Planting not found"));

        planting.setCurrentYieldPrediction(yieldPrediction);
        cropPlantingRepository.save(planting);
    }

    public List<CropPlantingDTO> getActivePlantings(User user) {
        return cropPlantingRepository.findByUserAndStatus(user, CropPlanting.PlantingStatus.MONITORING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CropPlantingDTO convertToDTO(CropPlanting planting) {
        return CropPlantingDTO.fromEntity(planting);
    }
}
