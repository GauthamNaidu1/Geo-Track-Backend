package com.geotrack.dto.auth;

import com.geotrack.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateEmployeeRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;
    
    // organizationId is intentionally omitted; it should be derived from the authenticated creator (or explicitly provided by SUPER_ADMIN)
    private UUID organizationId; // SUPER_ADMIN can provide this to create an employee in a specific organization

    private UUID departmentId;
    private UUID managerId;
    private UUID shiftId;
    private UUID officeId;
}
