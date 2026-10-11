package com.geotrack.controller;

import com.geotrack.entity.Office;
import com.geotrack.entity.Organization;
import com.geotrack.repository.OfficeRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/offices")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficeRepository officeRepository;
    private final com.geotrack.repository.OrganizationRepository organizationRepository;

    @GetMapping
    public ResponseEntity<?> getAllOffices(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        List<Office> offices = officeRepository.findByOrganizationId(user.getEmployee().getOrganization().getId());
        return ResponseEntity.ok(Map.of("offices", offices));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOffice(@PathVariable UUID id) {
        return officeRepository.findById(id)
                .map(office -> ResponseEntity.ok(Map.of("office", office)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createOffice(@RequestBody Office office, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        Organization org = organizationRepository.findById(user.getEmployee().getOrganization().getId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        office.setOrganization(org);
        try {
            Office saved = officeRepository.save(office);
            return ResponseEntity.ok(Map.of("message", "Office created", "office", saved));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "An office with this name already exists."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOffice(@PathVariable UUID id, @RequestBody Office office) {
        return officeRepository.findById(id).map(existing -> {
            existing.setName(office.getName());
            existing.setAddress(office.getAddress());
            existing.setLatitude(office.getLatitude());
            existing.setLongitude(office.getLongitude());
            existing.setRadiusMeters(office.getRadiusMeters());
            existing.setTimezone(office.getTimezone());
            Office saved = officeRepository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Office updated", "office", saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOffice(@PathVariable UUID id) {
        officeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Office deleted"));
    }
}
