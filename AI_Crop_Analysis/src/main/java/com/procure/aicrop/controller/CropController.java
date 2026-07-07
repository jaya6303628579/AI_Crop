package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.entity.Crop;
import com.procure.aicrop.service.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class CropController {

    private final CropService cropService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Crop>>> getAllCrops() {
        List<Crop> crops = cropService.getAllCrops();
        return ResponseEntity.ok(ApiResponse.success("Crops retrieved successfully", crops));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Crop>> getCropById(@PathVariable Long id) {
        try {
            Crop crop = cropService.getCropById(id)
                    .orElseThrow(() -> new RuntimeException("Crop not found"));
            return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", crop));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CROP_NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<Crop>> getCropByName(@PathVariable String name) {
        try {
            Crop crop = cropService.getCropByName(name)
                    .orElseThrow(() -> new RuntimeException("Crop not found"));
            return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", crop));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CROP_NOT_FOUND", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Crop>> createCrop(@RequestBody Crop crop) {
        try {
            Crop createdCrop = cropService.createCrop(crop);
            return ResponseEntity.ok(ApiResponse.success("Crop created successfully", createdCrop));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CROP_CREATION_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Crop>> updateCrop(@PathVariable Long id, @RequestBody Crop cropDetails) {
        try {
            Crop updatedCrop = cropService.updateCrop(id, cropDetails);
            return ResponseEntity.ok(ApiResponse.success("Crop updated successfully", updatedCrop));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CROP_UPDATE_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/soil/{soilType}")
    public ResponseEntity<ApiResponse<List<Crop>>> getCropsBySoilType(@PathVariable String soilType) {
        List<Crop> crops = cropService.getCropsBySuitableForSoil(soilType);
        return ResponseEntity.ok(ApiResponse.success("Crops retrieved successfully", crops));
    }

    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<List<Crop>>> discoverMoreCrops() {
        List<Crop> added = cropService.discoverMoreCrops();
        return ResponseEntity.ok(ApiResponse.success(
                added.isEmpty() ? "No new crops found" : added.size() + " new crops added", added));
    }
}
