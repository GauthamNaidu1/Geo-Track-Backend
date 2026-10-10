package com.geotrack.controller;

import com.geotrack.entity.Department;
import com.geotrack.repository.DepartmentRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    public ResponseEntity<?> getAllDepartments(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        List<Department> departments = departmentRepository.findByOrganizationId(user.getEmployee().getOrganization().getId());
        return ResponseEntity.ok(Map.of("departments", departments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartment(@PathVariable UUID id) {
        return departmentRepository.findById(id)
                .map(dept -> ResponseEntity.ok(Map.of("department", dept)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Department department, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        department.setOrganization(user.getEmployee().getOrganization());
        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(Map.of("message", "Department created", "department", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable UUID id, @RequestBody Department department) {
        return departmentRepository.findById(id).map(existing -> {
            existing.setName(department.getName());
            existing.setDescription(department.getDescription());
            Department saved = departmentRepository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Department updated", "department", saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable UUID id) {
        departmentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Department deleted"));
    }
}
