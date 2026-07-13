package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.dto.CropPlantingDTO;
import com.procure.aicrop.dto.SoilAnalysisDTO;
import com.procure.aicrop.dto.SowingValidationDTO;
import com.procure.aicrop.entity.*;
import com.procure.aicrop.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plantings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class CropPlantingController {

    private final CropPlantingService cropPlantingService;
    private final UserService userService;
    private final CropService cropService;
    private final SoilAnalysisService soilAnalysisService;
    private final RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CropPlantingDTO>> createPlanting(
            @RequestBody Map<String, Object> request) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            Long cropId = toLong(request.get("cropId"));
            String sowingDateStr = (String) request.get("sowingDate");
            Double areaPlanted = toDouble(request.get("areaPlanted"));

            LocalDate sowingDate = LocalDate.parse(sowingDateStr);

            Long soilAnalysisId = request.containsKey("soilAnalysisId") ?
                    toLong(request.get("soilAnalysisId")) : null;

            SoilAnalysis soilAnalysis = null;
            if (soilAnalysisId != null) {
                SoilAnalysisDTO soilDTO = soilAnalysisService.getSoilAnalysis(soilAnalysisId, user);
                soilAnalysis = new SoilAnalysis();
                soilAnalysis.setId(soilDTO.getId());
                soilAnalysis.setUser(user);
                soilAnalysis.setSoilType(soilDTO.getSoilType());
                soilAnalysis.setPH(soilDTO.getPH());
                soilAnalysis.setNitrogen(soilDTO.getNitrogen());
                soilAnalysis.setPhosphorus(soilDTO.getPhosphorus());
                soilAnalysis.setPotassium(soilDTO.getPotassium());
                soilAnalysis.setOrganicMatter(soilDTO.getOrganicMatter());
                soilAnalysis.setTexture(soilDTO.getTexture());
                soilAnalysis.setMoisture(soilDTO.getMoisture());
                soilAnalysis.setConfidence(soilDTO.getConfidence());
                soilAnalysis.setAnalysis(soilDTO.getAnalysis());
            }

            CropPlantingDTO planting = cropPlantingService.createCropPlanting(
                    user, cropId, sowingDate, areaPlanted, soilAnalysis, null);

            return ResponseEntity.ok(ApiResponse.success("Crop planting created successfully", planting));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PLANTING_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/validate-sowing")
    public ResponseEntity<ApiResponse<SowingValidationDTO>> validateSowing(
            @RequestBody Map<String, Object> request) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            Long cropId = toLong(request.get("cropId"));
            LocalDate plannedDate = LocalDate.parse((String) request.get("sowingDate"));
            Double latitude = request.get("latitude") != null ? toDouble(request.get("latitude")) : null;
            Double longitude = request.get("longitude") != null ? toDouble(request.get("longitude")) : null;

            Crop crop = cropService.getCropById(cropId)
                    .orElseThrow(() -> new RuntimeException("Crop not found"));

            SowingValidationDTO validation = recommendationService.validateSowingPlan(
                    crop, plannedDate, latitude, longitude, user);

            return ResponseEntity.ok(ApiResponse.success("Sowing plan validated", validation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_FAILED", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropPlantingDTO>>> getUserPlantings() {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            List<CropPlantingDTO> plantings = cropPlantingService.getUserPlantings(user);
            return ResponseEntity.ok(ApiResponse.success("Plantings retrieved successfully", plantings));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{plantingId}")
    public ResponseEntity<ApiResponse<CropPlantingDTO>> getPlanting(
            @PathVariable Long plantingId) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            CropPlantingDTO planting = cropPlantingService.getPlanting(plantingId, user);
            return ResponseEntity.ok(ApiResponse.success("Planting retrieved successfully", planting));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PLANTING_NOT_FOUND", e.getMessage()));
        }
    }

    @PutMapping("/{plantingId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long plantingId,
            @RequestParam String status) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            CropPlanting.PlantingStatus plantingStatus = CropPlanting.PlantingStatus.valueOf(status);
            cropPlantingService.updatePlantingStatus(plantingId, plantingStatus, user);

            return ResponseEntity.ok(ApiResponse.success("Planting status updated successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{plantingId}/growth-stage")
    public ResponseEntity<ApiResponse<String>> updateGrowthStage(
            @PathVariable Long plantingId,
            @RequestParam String stage) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            CropPlanting.GrowthStage growthStage = CropPlanting.GrowthStage.valueOf(stage);
            cropPlantingService.updateGrowthStage(plantingId, growthStage, user);

            return ResponseEntity.ok(ApiResponse.success("Growth stage updated successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CropPlantingDTO>>> getActivePlantings() {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            List<CropPlantingDTO> plantings = cropPlantingService.getActivePlantings(user);
            return ResponseEntity.ok(ApiResponse.success("Active plantings retrieved", plantings));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }


    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        return Double.valueOf(value.toString());
    }
}
