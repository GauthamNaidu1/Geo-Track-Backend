package com.geotrack.dto.auth;

import com.geotrack.entity.enums.Role;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateEmployeeRequest {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private UUID departmentId;
    private UUID managerId;
    private UUID shiftId;
    private UUID officeId;
}
