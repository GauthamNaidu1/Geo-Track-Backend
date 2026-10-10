package com.geotrack.controller;

import com.geotrack.dto.auth.CreateOrganizationRequest;
import com.geotrack.entity.Organization;
import com.geotrack.service.OrganizationManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationManagementService organizationManagementService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(organizationManagementService.createOrganization(request));
    }
}
