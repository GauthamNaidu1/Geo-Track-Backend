package com.geotrack.repository;

import com.geotrack.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByOrganizationIdAndEmail(UUID organizationId, String email);
    long countByOrganizationId(UUID organizationId);
    java.util.List<Employee> findByOrganizationId(UUID organizationId);
    org.springframework.data.domain.Page<Employee> findByOrganizationId(UUID organizationId, org.springframework.data.domain.Pageable pageable);
}
