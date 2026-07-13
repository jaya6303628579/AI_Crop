package com.procure.aicrop.controller;

import com.procure.aicrop.dto.AlertDTO;
import com.procure.aicrop.dto.ApiResponse;
import com.procure.aicrop.entity.User;
import com.procure.aicrop.service.AlertService;
import com.procure.aicrop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AlertController {

    private final AlertService alertService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getUserAlerts() {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            List<AlertDTO> alerts = alertService.getUserAlerts(user);
            return ResponseEntity.ok(ApiResponse.success("Alerts retrieved successfully", alerts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getUnreadAlerts() {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            List<AlertDTO> alerts = alertService.getUnreadAlerts(user);
            return ResponseEntity.ok(ApiResponse.success("Unread alerts retrieved", alerts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/critical")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getCriticalAlerts() {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            List<AlertDTO> alerts = alertService.getCriticalAlerts(user);
            return ResponseEntity.ok(ApiResponse.success("Critical alerts retrieved", alerts));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RETRIEVAL_ERROR", e.getMessage()));
        }
    }

    @PutMapping("/{alertId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long alertId) {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            alertService.markAsRead(alertId, user);
            return ResponseEntity.ok(ApiResponse.success("Alert marked as read", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            alertService.markAllAsRead(user);
            return ResponseEntity.ok(ApiResponse.success("All alerts marked as read", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
        }
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<ApiResponse<String>> deleteAlert(@PathVariable Long alertId) {

        try {
            User user = userService.getCurrentAuthenticatedUser();
            alertService.deleteAlert(alertId, user);
            return ResponseEntity.ok(ApiResponse.success("Alert deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DELETE_FAILED", e.getMessage()));
        }
    }

}
