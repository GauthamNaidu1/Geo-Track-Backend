package com.geotrack.repository;

import com.geotrack.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByOrganizationId(UUID organizationId);
}
