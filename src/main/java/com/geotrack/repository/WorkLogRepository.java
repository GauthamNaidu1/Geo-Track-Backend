package com.geotrack.repository;

import com.geotrack.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, UUID> {
    List<WorkLog> findByEmployeeIdOrderByWorkDateDesc(UUID employeeId);
    List<WorkLog> findByOrganizationIdOrderByWorkDateDesc(UUID organizationId);
    java.util.Optional<WorkLog> findByEmployeeIdAndWorkDate(UUID employeeId, java.time.LocalDate workDate);
}
