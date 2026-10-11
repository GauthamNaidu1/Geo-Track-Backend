package com.geotrack.controller;

import com.geotrack.entity.LeaveType;
import com.geotrack.entity.Organization;
import com.geotrack.repository.LeaveTypeRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeRepository leaveTypeRepository;
    private final com.geotrack.repository.OrganizationRepository organizationRepository;

    @GetMapping
    public ResponseEntity<?> getAllLeaveTypes(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        List<LeaveType> leaveTypes = leaveTypeRepository.findByOrganizationId(user.getEmployee().getOrganization().getId());
        return ResponseEntity.ok(Map.of("leaveTypes", leaveTypes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLeaveType(@PathVariable UUID id) {
        return leaveTypeRepository.findById(id)
                .map(leaveType -> ResponseEntity.ok(Map.of("leaveType", leaveType)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createLeaveType(@RequestBody LeaveType leaveType, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        Organization org = organizationRepository.findById(user.getEmployee().getOrganization().getId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        leaveType.setOrganization(org);
        LeaveType saved = leaveTypeRepository.save(leaveType);
        return ResponseEntity.ok(Map.of("message", "Leave Type created", "leaveType", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLeaveType(@PathVariable UUID id, @RequestBody LeaveType leaveType) {
        return leaveTypeRepository.findById(id).map(existing -> {
            existing.setName(leaveType.getName());
            existing.setDescription(leaveType.getDescription());
            existing.setDaysAllowed(leaveType.getDaysAllowed());
            existing.setRequiresApproval(leaveType.getRequiresApproval());
            LeaveType saved = leaveTypeRepository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Leave Type updated", "leaveType", saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLeaveType(@PathVariable UUID id) {
        leaveTypeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Leave Type deleted"));
    }
}
