package com.geotrack.repository;

import com.geotrack.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT w FROM WorkLog w JOIN FETCH w.employee WHERE w.employee.id = :employeeId ORDER BY w.workDate DESC")
    List<WorkLog> findByEmployeeIdOrderByWorkDateDesc(@org.springframework.data.repository.query.Param("employeeId") UUID employeeId);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM WorkLog w JOIN FETCH w.employee WHERE w.organization.id = :organizationId ORDER BY w.workDate DESC")
    List<WorkLog> findByOrganizationIdOrderByWorkDateDesc(@org.springframework.data.repository.query.Param("organizationId") UUID organizationId);

    java.util.Optional<WorkLog> findByEmployeeIdAndWorkDate(UUID employeeId, java.time.LocalDate workDate);
}
