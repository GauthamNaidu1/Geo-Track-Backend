package com.geotrack.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeDetailResponse {
    private String id;
    private String employeeCode;
    private String name;
    private String email;
    private String role;
    private String departmentId;
    private String managerId;
    private String shiftId;
    private String officeId;
}
