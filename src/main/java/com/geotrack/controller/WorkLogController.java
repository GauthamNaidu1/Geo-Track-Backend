package com.geotrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/work-logs")
public class WorkLogController {

    @Autowired
    private com.geotrack.repository.WorkLogRepository workLogRepository;

    @PostMapping
    public ResponseEntity<?> submitWorkLog(@RequestBody java.util.Map<String, String> payload, @org.springframework.security.core.annotation.AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();

        String workSummary = payload.get("workSummary");
        String blockers = payload.get("blockers");

        if (workSummary == null || workSummary.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Work summary is required"));
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        com.geotrack.entity.WorkLog log = workLogRepository.findByEmployeeIdAndWorkDate(user.getEmployee().getId(), today)
                .orElseGet(() -> {
                    com.geotrack.entity.WorkLog newLog = new com.geotrack.entity.WorkLog();
                    newLog.setEmployee(user.getEmployee());
                    newLog.setOrganization(user.getEmployee().getOrganization());
                    newLog.setWorkDate(today);
                    return newLog;
                });
        
        log.setSummary(workSummary.trim());
        log.setBlockers(blockers != null && !blockers.trim().isEmpty() ? blockers.trim() : null);

        workLogRepository.save(log);

        return ResponseEntity.ok(java.util.Map.of("message", "Work log submitted successfully"));
    }

    @GetMapping
    public ResponseEntity<?> getAllWorkLogs(@org.springframework.security.core.annotation.AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        java.util.List<com.geotrack.entity.WorkLog> logs = workLogRepository.findByOrganizationIdOrderByWorkDateDesc(user.getEmployee().getOrganization().getId());
        return ResponseEntity.ok(formatResponse(logs));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyWorkLogs(@org.springframework.security.core.annotation.AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        java.util.List<com.geotrack.entity.WorkLog> logs = workLogRepository.findByEmployeeIdOrderByWorkDateDesc(user.getEmployee().getId());
        return ResponseEntity.ok(formatResponse(logs));
    }

    @GetMapping("/team")
    public ResponseEntity<?> getTeamWorkLogs(@org.springframework.security.core.annotation.AuthenticationPrincipal com.geotrack.security.CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        java.util.List<com.geotrack.entity.WorkLog> logs = workLogRepository.findByOrganizationIdOrderByWorkDateDesc(user.getEmployee().getOrganization().getId());
        return ResponseEntity.ok(formatResponse(logs));
    }

    private java.util.Map<String, Object> formatResponse(java.util.List<com.geotrack.entity.WorkLog> logs) {
        return java.util.Map.of(
                "workLogs", formatLogs(logs),
                "pagination", java.util.Map.of(
                        "total", logs.size(),
                        "page", 1,
                        "pages", 1,
                        "limit", 100
                )
        );
    }

    private java.util.List<java.util.Map<String, Object>> formatLogs(java.util.List<com.geotrack.entity.WorkLog> logs) {
        return logs.stream().map(l -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", l.getId().toString());
            map.put("createdAt", l.getCreatedAt() != null ? l.getCreatedAt().toString() : l.getWorkDate().atStartOfDay().toString());
            map.put("workDate", l.getWorkDate().toString());
            map.put("workSummary", l.getSummary());
            map.put("blockers", l.getBlockers());
            map.put("employee", java.util.Map.of(
                    "name", l.getEmployee().getFirstName() + " " + (l.getEmployee().getLastName() != null ? l.getEmployee().getLastName() : ""),
                    "email", l.getEmployee().getEmail(),
                    "employeeCode", "EMP-" + l.getEmployee().getId().toString().substring(0,4).toUpperCase()
            ));
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }
}
