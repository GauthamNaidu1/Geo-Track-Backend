package com.geotrack.controller;

import com.geotrack.dto.auth.CreateEmployeeRequest;
import com.geotrack.dto.auth.UserResponse;
import com.geotrack.security.CustomUserDetails;
import com.geotrack.service.EmployeeManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeManagementService employeeManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(employeeManagementService.createEmployee(request, userDetails));
    }

    @GetMapping
    public ResponseEntity<com.geotrack.dto.EmployeeListResponse> getAllEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getEmployee() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(employeeManagementService.getAllEmployees(userDetails.getEmployee().getOrganization().getId(), page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<java.util.Map<String, Object>> getEmployee(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(java.util.Map.of("employee", employeeManagementService.getEmployee(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateEmployee(
            @PathVariable java.util.UUID id,
            @Valid @RequestBody com.geotrack.dto.auth.UpdateEmployeeRequest request) {
        employeeManagementService.updateEmployee(id, request);
        return ResponseEntity.ok(java.util.Map.of("message", "Employee updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteEmployee(@PathVariable java.util.UUID id) {
        employeeManagementService.deleteEmployee(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Employee deleted successfully"));
    }
}
