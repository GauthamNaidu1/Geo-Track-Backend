package com.geotrack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications() {
        // Return empty list to prevent 404
        return ResponseEntity.ok(Map.of("notifications", List.of()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }
}
