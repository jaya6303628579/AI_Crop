package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.dto.CropRecommendationDTO;
import com.procure.aicrop.dto.SoilAnalysisDTO;
import com.procure.aicrop.entity.*;
import com.procure.aicrop.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserService userService;
    private final CropService cropService;
    private final SoilAnalysisService soilAnalysisService;

    @PostMapping("/generate/{cropId}")
    public ResponseEntity<ApiResponse<CropRecommendationDTO>> generateRecommendation(
            @PathVariable Long cropId,
            @RequestParam Long soilAnalysisId) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            Crop crop = cropService.getCropById(cropId)
                    .orElseThrow(() -> new RuntimeException("Crop not found"));

            SoilAnalysisDTO soilDTO = soilAnalysisService.getSoilAnalysis(soilAnalysisId, user);
            SoilAnalysis soil = new SoilAnalysis();
            soil.setId(soilDTO.getId());
            soil.setUser(user);
            soil.setSoilType(soilDTO.getSoilType());
            soil.setPH(soilDTO.getPH());
            soil.setNitrogen(soilDTO.getNitrogen());
            soil.setPhosphorus(soilDTO.getPhosphorus());
            soil.setPotassium(soilDTO.getPotassium());
            soil.setOrganicMatter(soilDTO.getOrganicMatter());
            soil.setTexture(soilDTO.getTexture());
            soil.setMoisture(soilDTO.getMoisture());
            soil.setConfidence(soilDTO.getConfidence());
            soil.setAnalysis(soilDTO.getAnalysis());

            CropRecommendationDTO recommendation = recommendationService
                    .generateCropSuitabilityRecommendation(user, crop, soil);

            return ResponseEntity.ok(ApiResponse.success("Recommendation generated successfully", recommendation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RECOMMENDATION_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/alternatives")
    public ResponseEntity<ApiResponse<List<CropRecommendationDTO>>> getAlternativeRecommendations(
            @RequestParam Long soilAnalysisId) {

        try {
            User user = userService.getCurrentAuthenticatedUser();

            SoilAnalysisDTO soilDTO = soilAnalysisService.getSoilAnalysis(soilAnalysisId, user);
            SoilAnalysis soil = new SoilAnalysis();
            soil.setId(soilDTO.getId());
            soil.setUser(user);
            soil.setSoilType(soilDTO.getSoilType());
            soil.setPH(soilDTO.getPH());
            soil.setNitrogen(soilDTO.getNitrogen());
            soil.setPhosphorus(soilDTO.getPhosphorus());
            soil.setPotassium(soilDTO.getPotassium());
            soil.setOrganicMatter(soilDTO.getOrganicMatter());
            soil.setTexture(soilDTO.getTexture());
            soil.setMoisture(soilDTO.getMoisture());
            soil.setConfidence(soilDTO.getConfidence());
            soil.setAnalysis(soilDTO.getAnalysis());

            List<CropRecommendationDTO> recommendations = recommendationService
                    .generateAlternativeCropRecommendations(user, soil);

            return ResponseEntity.ok(ApiResponse.success("Alternative recommendations retrieved", recommendations));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RECOMMENDATION_FAILED", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropRecommendationDTO>>> getUserRecommendations() {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            List<CropRecommendationDTO> recommendations = recommendationService.getUserRecommendations(user);
            return ResponseEntity.ok(ApiResponse.success("Recommendations retrieved successfully", recommendations));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/{recommendationId}/accept")
    public ResponseEntity<ApiResponse<String>> acceptRecommendation(
            @PathVariable Long recommendationId) {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            recommendationService.acceptRecommendation(recommendationId, user);
            return ResponseEntity.ok(ApiResponse.success("Recommendation accepted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ACCEPTANCE_FAILED", e.getMessage()));
        }
    }

}
