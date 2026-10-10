package com.geotrack.service;

import com.geotrack.dto.auth.CreateOrganizationRequest;
import com.geotrack.entity.Organization;
import com.geotrack.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationManagementService {
    private final OrganizationRepository organizationRepository;

    @Transactional
    public Organization createOrganization(CreateOrganizationRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setEmail(request.getEmail());
        org.setDescription(request.getDescription());
        org.setPhone(request.getPhone());
        org.setAddress(request.getAddress());
        
        return organizationRepository.save(org);
    }
}
