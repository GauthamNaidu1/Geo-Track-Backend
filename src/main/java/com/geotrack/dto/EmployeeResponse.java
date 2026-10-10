package com.geotrack.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeResponse {
    private String id;
    private String employeeCode;
    private String name;
    private String email;
    private String role;
    private IdNameDto department;
    private IdNameDto shift;
    private IdNameDto manager;
    private IdNameDto office;

    @Data
    @Builder
    public static class IdNameDto {
        private String id;
        private String name;
    }
}
