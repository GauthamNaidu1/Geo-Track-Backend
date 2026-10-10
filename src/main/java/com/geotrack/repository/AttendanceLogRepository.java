package com.geotrack.repository;

import com.geotrack.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    long countByEmployeeOrganizationIdAndCheckinTimeBetweenAndCheckoutTimeIsNull(UUID orgId, LocalDateTime start, LocalDateTime end);
    long countByEmployeeOrganizationIdAndCheckinTimeBetweenAndCheckoutTimeIsNotNull(UUID orgId, LocalDateTime start, LocalDateTime end);
    List<AttendanceLog> findByEmployeeOrganizationIdAndCheckinTimeBetweenOrderByCheckinTimeDesc(UUID orgId, LocalDateTime start, LocalDateTime end);
}
