package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.entity.CropPlanting;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.entity.YieldPrediction;
import com.procure.aicrop.repository.CropPlantingRepository;
import com.procure.aicrop.repository.YieldPredictionRepository;
import com.procure.aicrop.service.UserService;
import com.procure.aicrop.service.YieldPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/yield-predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class YieldPredictionController {

    private final YieldPredictionService yieldPredictionService;
    private final UserService userService;
    private final CropPlantingRepository cropPlantingRepository;
    private final YieldPredictionRepository yieldPredictionRepository;

    @PostMapping("/{plantingId}/generate")
    public ResponseEntity<ApiResponse<YieldPrediction>> generatePrediction(
            @PathVariable Long plantingId) {

        try {
            User user = getOrCreateDefaultUser();

            CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                    .orElseThrow(() -> new RuntimeException("Planting not found"));

            YieldPrediction prediction = yieldPredictionService.generateDailyYieldPrediction(planting, user);

            return ResponseEntity.ok(ApiResponse.success("Yield prediction generated successfully", prediction));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PREDICTION_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/{plantingId}")
    public ResponseEntity<ApiResponse<List<YieldPrediction>>> getPredictions(
            @PathVariable Long plantingId) {

        try {
            User user = getOrCreateDefaultUser();

            CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                    .orElseThrow(() -> new RuntimeException("Planting not found"));

            List<YieldPrediction> predictions = yieldPredictionRepository
                    .findByCropPlantingOrderByPredictedAtDesc(planting);

            return ResponseEntity.ok(ApiResponse.success("Predictions retrieved successfully", predictions));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{plantingId}/latest")
    public ResponseEntity<ApiResponse<YieldPrediction>> getLatestPrediction(
            @PathVariable Long plantingId) {

        try {
            User user = getOrCreateDefaultUser();

            CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                    .orElseThrow(() -> new RuntimeException("Planting not found"));

            YieldPrediction prediction = yieldPredictionRepository
                    .findFirstByCropPlantingOrderByPredictedAtDesc(planting)
                    .orElseThrow(() -> new RuntimeException("No predictions found"));

            return ResponseEntity.ok(ApiResponse.success("Latest prediction retrieved", prediction));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PREDICTION_NOT_FOUND", e.getMessage()));
        }
    }

    private User getOrCreateDefaultUser() {
        String defaultEmail = "default@aicrop.com";
        return userService.findByEmail(defaultEmail)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(defaultEmail)
                            .fullName("Public User")
                            .phoneNumber("0000000000")
                            .password("default")
                            .role(User.UserRole.FARMER)
                            .active(true)
                            .build();
                    return userService.createUser(newUser);
                });
    }
}
