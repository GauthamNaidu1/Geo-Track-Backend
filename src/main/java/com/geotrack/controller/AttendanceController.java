package com.geotrack.controller;

import com.geotrack.entity.AttendanceLog;
import com.geotrack.entity.Office;
import com.geotrack.entity.enums.AttendanceStatus;
import com.geotrack.repository.AttendanceLogRepository;
import com.geotrack.repository.OfficeRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceLogRepository attendanceLogRepository;
    private final OfficeRepository officeRepository;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        long activeLogs = attendanceLogRepository.countByEmployeeOrganizationIdAndCheckinTimeBetweenAndCheckoutTimeIsNull(
                user.getEmployee().getOrganization().getId(), startOfDay, endOfDay);
        
        return ResponseEntity.ok(Map.of("isCheckedIn", activeLogs > 0));
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkin(@RequestBody Map<String, Object> body, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();

        String officeIdStr = (String) body.get("officeId");
        Double latitude = ((Number) body.get("latitude")).doubleValue();
        Double longitude = ((Number) body.get("longitude")).doubleValue();

        Office office = officeRepository.findById(UUID.fromString(officeIdStr)).orElseThrow();

        // Calculate distance (very rough approximation for testing)
        double distance = calculateDistance(latitude, longitude, office.getLatitude(), office.getLongitude());
        if (distance > office.getRadiusMeters()) {
            return ResponseEntity.badRequest().body(Map.of("message", "You are too far from the office", "distance", distance));
        }

        AttendanceLog log = new AttendanceLog();
        log.setEmployee(user.getEmployee());
        log.setOrganization(user.getEmployee().getOrganization());
        log.setOffice(office);
        log.setCheckinTime(LocalDateTime.now());
        log.setCheckinLatitude(latitude);
        log.setCheckinLongitude(longitude);
        log.setDistanceFromOffice(distance);
        log.setStatus(AttendanceStatus.PRESENT);

        attendanceLogRepository.save(log);

        return ResponseEntity.ok(Map.of("message", "Checked in successfully"));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> body, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();

        Double latitude = body.get("latitude") != null ? ((Number) body.get("latitude")).doubleValue() : null;
        Double longitude = body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null;

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<AttendanceLog> logs = attendanceLogRepository.findByEmployeeOrganizationIdAndCheckinTimeBetweenOrderByCheckinTimeDesc(
                user.getEmployee().getOrganization().getId(), startOfDay, endOfDay);

        AttendanceLog activeLog = logs.stream().filter(l -> l.getCheckoutTime() == null).findFirst().orElse(null);

        if (activeLog == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active checkin found"));
        }

        activeLog.setCheckoutTime(LocalDateTime.now());
        activeLog.setCheckoutLatitude(latitude);
        activeLog.setCheckoutLongitude(longitude);
        
        attendanceLogRepository.save(activeLog);

        return ResponseEntity.ok(Map.of("message", "Checked out successfully"));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }
}
