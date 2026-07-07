package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.dto.SoilAnalysisDTO;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.service.SoilAnalysisService;
import com.procure.aicrop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/soil")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SoilAnalysisController {

    private final SoilAnalysisService soilAnalysisService;
    private final UserService userService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<SoilAnalysisDTO>> analyzeSoil(
            @RequestParam("image") MultipartFile imageFile) {

        try {
            User user = getOrCreateDefaultUser();

            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_FILE", "Image file is required"));
            }

            SoilAnalysisDTO analysis = soilAnalysisService.analyzeSoil(user, imageFile);
            return ResponseEntity.ok(ApiResponse.success("Soil analyzed successfully", analysis));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FILE_UPLOAD_ERROR", "Failed to upload image: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ANALYSIS_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/analyses")
    public ResponseEntity<ApiResponse<List<SoilAnalysisDTO>>> getUserAnalyses() {

        try {
            User user = getOrCreateDefaultUser();
            List<SoilAnalysisDTO> analyses = soilAnalysisService.getUserSoilAnalyses(user);
            return ResponseEntity.ok(ApiResponse.success("Analyses retrieved successfully", analyses));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<SoilAnalysisDTO>> getLatestAnalysis() {

        try {
            User user = getOrCreateDefaultUser();
            Optional<SoilAnalysisDTO> analysis = soilAnalysisService.getLatestSoilAnalysis(user);

            if (analysis.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("NO_ANALYSIS", "No soil analysis found"));
            }

            return ResponseEntity.ok(ApiResponse.success("Latest analysis retrieved", analysis.get()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<SoilAnalysisDTO>> getAnalysis(@PathVariable Long analysisId) {

        try {
            User user = getOrCreateDefaultUser();
            SoilAnalysisDTO analysis = soilAnalysisService.getSoilAnalysis(analysisId, user);
            return ResponseEntity.ok(ApiResponse.success("Analysis retrieved successfully", analysis));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ANALYSIS_NOT_FOUND", e.getMessage()));
        }
    }

    @DeleteMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<String>> deleteAnalysis(@PathVariable Long analysisId) {

        try {
            User user = getOrCreateDefaultUser();
            soilAnalysisService.deleteSoilAnalysis(analysisId, user);
            return ResponseEntity.ok(ApiResponse.success("Analysis deleted successfully", null));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DELETE_ERROR", "Failed to delete analysis: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DELETE_ERROR", e.getMessage()));
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
