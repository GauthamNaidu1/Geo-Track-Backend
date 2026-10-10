package com.geotrack.controller;

import com.geotrack.entity.Shift;
import com.geotrack.repository.ShiftRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftRepository shiftRepository;

    @GetMapping
    public ResponseEntity<?> getAllShifts(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        List<Shift> shifts = shiftRepository.findByOrganizationId(user.getEmployee().getOrganization().getId());
        return ResponseEntity.ok(Map.of("shifts", shifts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getShift(@PathVariable UUID id) {
        return shiftRepository.findById(id)
                .map(shift -> ResponseEntity.ok(Map.of("shift", shift)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createShift(@RequestBody Shift shift, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        shift.setOrganization(user.getEmployee().getOrganization());
        Shift saved = shiftRepository.save(shift);
        return ResponseEntity.ok(Map.of("message", "Shift created", "shift", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateShift(@PathVariable UUID id, @RequestBody Shift shift) {
        return shiftRepository.findById(id).map(existing -> {
            existing.setName(shift.getName());
            existing.setStartTime(shift.getStartTime());
            existing.setEndTime(shift.getEndTime());
            existing.setGracePeriodMinutes(shift.getGracePeriodMinutes());
            Shift saved = shiftRepository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Shift updated", "shift", saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShift(@PathVariable UUID id) {
        shiftRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Shift deleted"));
    }
}
