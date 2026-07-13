package com.procure.aicrop.controller;

import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.entity.CropPlanting;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.repository.CropPlantingRepository;
import com.procure.aicrop.scheduler.CropMonitoringScheduler;
import com.procure.aicrop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class MonitoringController {

    private final CropMonitoringScheduler cropMonitoringScheduler;
    private final CropPlantingRepository cropPlantingRepository;
    private final UserService userService;

    @PostMapping("/run-now")
    public ResponseEntity<ApiResponse<String>> runNow() {
        cropMonitoringScheduler.runDailyMonitoring();
        return ResponseEntity.ok(ApiResponse.success("Daily monitoring run triggered", null));
    }

    @PostMapping("/plantings/{plantingId}/run")
    public ResponseEntity<ApiResponse<String>> runForPlanting(@PathVariable Long plantingId) {
        try {
            User user = userService.getCurrentAuthenticatedUser();
            CropPlanting planting = cropPlantingRepository.findByIdAndUser(plantingId, user)
                    .orElseThrow(() -> new RuntimeException("Planting not found"));

            cropMonitoringScheduler.monitorPlanting(planting);
            return ResponseEntity.ok(ApiResponse.success("Monitoring run for planting completed", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("MONITORING_FAILED", e.getMessage()));
        }
    }

}
