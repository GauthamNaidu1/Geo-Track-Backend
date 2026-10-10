package com.geotrack.repository;

import com.geotrack.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.time.LocalDate;
import com.geotrack.entity.enums.LeaveStatus;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    long countByOrganizationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(UUID orgId, LeaveStatus status, LocalDate today1, LocalDate today2);
    java.util.List<LeaveRequest> findByOrganizationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(UUID orgId, LeaveStatus status, LocalDate today1, LocalDate today2);
    org.springframework.data.domain.Page<LeaveRequest> findByOrganizationId(UUID orgId, org.springframework.data.domain.Pageable pageable);
}
