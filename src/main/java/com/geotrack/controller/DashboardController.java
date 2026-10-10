package com.geotrack.controller;

import com.geotrack.dto.DashboardStats;
import com.geotrack.dto.LiveAttendanceResponse;
import com.geotrack.entity.Employee;
import com.geotrack.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(@AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null || user.getEmployee().getOrganization() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dashboardService.getStats(user.getEmployee().getOrganization().getId()));
    }

    @GetMapping("/live-attendance")
    public ResponseEntity<LiveAttendanceResponse> getLiveAttendance(@AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null || user.getEmployee().getOrganization() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dashboardService.getLiveAttendance(user.getEmployee().getOrganization().getId()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        return ResponseEntity.ok(java.util.Map.of("logs", java.util.List.of()));
    }

    @GetMapping("/team-status")
    public ResponseEntity<?> getTeamStatus(@AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null || user.getEmployee().getOrganization() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dashboardService.getTeamStatus(user.getEmployee().getOrganization().getId()));
    }

    @GetMapping("/team-productivity-trends")
    public ResponseEntity<?> getTeamProductivityTrends() {
        return ResponseEntity.ok(java.util.Map.of("trends", java.util.List.of()));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(java.util.List.of());
    }
}
