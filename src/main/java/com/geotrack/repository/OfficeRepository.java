package com.geotrack.repository;

import com.geotrack.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.List;

public interface OfficeRepository extends JpaRepository<Office, UUID> {
    List<Office> findByOrganizationId(UUID organizationId);
}
