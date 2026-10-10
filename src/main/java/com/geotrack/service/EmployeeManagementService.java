package com.geotrack.service;

import com.geotrack.dto.auth.CreateEmployeeRequest;
import com.geotrack.dto.auth.UserResponse;
import com.geotrack.entity.Employee;
import com.geotrack.entity.Organization;
import com.geotrack.entity.enums.Role;
import com.geotrack.repository.EmployeeRepository;
import com.geotrack.repository.OrganizationRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.geotrack.dto.EmployeeResponse;
import com.geotrack.dto.EmployeeListResponse;

@Service
@RequiredArgsConstructor
public class EmployeeManagementService {
    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final com.geotrack.repository.LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createEmployee(CreateEmployeeRequest request, CustomUserDetails currentUser) {
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Enforce RBAC
        if (request.getRole() == Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Cannot create SUPER_ADMIN");
        }
        
        if (request.getRole() == Role.ORG_ADMIN && currentUser.getEmployee().getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only SUPER_ADMIN can create ORG_ADMIN");
        }

        UUID targetOrgId = request.getOrganizationId();
        
        if (currentUser.getEmployee().getRole() != Role.SUPER_ADMIN) {
            // ORG_ADMIN or MANAGER can only create in their own org
            targetOrgId = currentUser.getEmployee().getOrganization().getId();
        } else if (targetOrgId == null) {
             throw new IllegalArgumentException("SUPER_ADMIN must specify an organizationId when creating employees");
        }

        Organization org = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setOrganization(org);
        employee.setRole(request.getRole());

        if (request.getDepartmentId() != null) {
            employee.setDepartment(new com.geotrack.entity.Department());
            employee.getDepartment().setId(request.getDepartmentId());
        }
        if (request.getShiftId() != null) {
            employee.setShift(new com.geotrack.entity.Shift());
            employee.getShift().setId(request.getShiftId());
        }
        if (request.getManagerId() != null) {
            employee.setManager(new com.geotrack.entity.Employee());
            employee.getManager().setId(request.getManagerId());
        }
        if (request.getOfficeId() != null) {
            employee.setOffice(new com.geotrack.entity.Office());
            employee.getOffice().setId(request.getOfficeId());
        }

        Employee saved = employeeRepository.save(employee);
        
        return UserResponse.builder()
                .id(saved.getId())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .name(saved.getFirstName() + " " + saved.getLastName())
                .email(saved.getEmail())
                .organizationId(saved.getOrganization().getId())
                .employeeCode("EMP-" + saved.getId().toString().substring(0, 6).toUpperCase())
                .role(saved.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeListResponse getAllEmployees(UUID organizationId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Employee> employeePage = employeeRepository.findByOrganizationId(organizationId, pageable);

        List<EmployeeResponse> employees = employeePage.getContent().stream().map(emp -> 
            EmployeeResponse.builder()
                .id(emp.getId().toString())
                .employeeCode("EMP-" + emp.getId().toString().substring(0, 6).toUpperCase())
                .name(emp.getFirstName() + " " + emp.getLastName())
                .email(emp.getEmail())
                .role(emp.getRole() != null ? emp.getRole().name().toLowerCase() : null)
                .department(emp.getDepartment() != null ? 
                    EmployeeResponse.IdNameDto.builder().id(emp.getDepartment().getId().toString()).name(emp.getDepartment().getName()).build() : null)
                .shift(emp.getShift() != null ? 
                    EmployeeResponse.IdNameDto.builder().id(emp.getShift().getId().toString()).name(emp.getShift().getName()).build() : null)
                .office(emp.getOffice() != null ? 
                    EmployeeResponse.IdNameDto.builder().id(emp.getOffice().getId().toString()).name(emp.getOffice().getName()).build() : null)
                .build()
        ).collect(Collectors.toList());

        return EmployeeListResponse.builder()
                .employees(employees)
                .pagination(EmployeeListResponse.Pagination.builder()
                        .page(page)
                        .limit(limit)
                        .total(employeePage.getTotalElements())
                        .totalPages(employeePage.getTotalPages())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public com.geotrack.dto.EmployeeDetailResponse getEmployee(UUID id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        return com.geotrack.dto.EmployeeDetailResponse.builder()
                .id(emp.getId().toString())
                .employeeCode("EMP-" + emp.getId().toString().substring(0, 6).toUpperCase())
                .name(emp.getFirstName() + " " + emp.getLastName())
                .email(emp.getEmail())
                .role(emp.getRole() != null ? emp.getRole().name().toLowerCase() : null)
                .departmentId(emp.getDepartment() != null ? emp.getDepartment().getId().toString() : null)
                .managerId(emp.getManager() != null ? emp.getManager().getId().toString() : null)
                .shiftId(emp.getShift() != null ? emp.getShift().getId().toString() : null)
                .officeId(emp.getOffice() != null ? emp.getOffice().getId().toString() : null)
                .build();
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        
        // Nullify manager reference for employees managed by this person
        java.util.List<Employee> subordinates = employeeRepository.findByOrganizationId(emp.getOrganization().getId(), org.springframework.data.domain.Pageable.unpaged())
            .stream()
            .filter(e -> e.getManager() != null && e.getManager().getId().equals(id))
            .collect(java.util.stream.Collectors.toList());
            
        for (Employee sub : subordinates) {
            sub.setManager(null);
            employeeRepository.save(sub);
        }

        // Nullify decisionBy reference for leave requests decided by this person
        java.util.List<com.geotrack.entity.LeaveRequest> leaves = leaveRequestRepository.findAll()
            .stream()
            .filter(l -> l.getDecisionBy() != null && l.getDecisionBy().getId().equals(id))
            .collect(java.util.stream.Collectors.toList());
        for (com.geotrack.entity.LeaveRequest leave : leaves) {
            leave.setDecisionBy(null);
            leaveRequestRepository.save(leave);
        }

        employeeRepository.delete(emp);
    }

    @Transactional
    public void updateEmployee(UUID id, com.geotrack.dto.auth.UpdateEmployeeRequest request) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        
        emp.setFirstName(request.getFirstName());
        emp.setLastName(request.getLastName());
        emp.setEmail(request.getEmail());
        if (request.getRole() != null) {
            emp.setRole(request.getRole());
        }

        if (request.getDepartmentId() != null) {
            emp.setDepartment(new com.geotrack.entity.Department());
            emp.getDepartment().setId(request.getDepartmentId());
        } else {
            emp.setDepartment(null);
        }

        if (request.getShiftId() != null) {
            emp.setShift(new com.geotrack.entity.Shift());
            emp.getShift().setId(request.getShiftId());
        } else {
            emp.setShift(null);
        }

        if (request.getManagerId() != null) {
            emp.setManager(new com.geotrack.entity.Employee());
            emp.getManager().setId(request.getManagerId());
        } else {
            emp.setManager(null);
        }

        if (request.getOfficeId() != null) {
            emp.setOffice(new com.geotrack.entity.Office());
            emp.getOffice().setId(request.getOfficeId());
        } else {
            emp.setOffice(null);
        }
        
        employeeRepository.save(emp);
    }
}
