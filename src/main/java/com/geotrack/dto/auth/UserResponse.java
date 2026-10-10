package com.geotrack.dto.auth;

import com.geotrack.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private UUID organizationId;
    private Role role;
    private String employeeCode;
    private String departmentName;
    private String officeName;
}
