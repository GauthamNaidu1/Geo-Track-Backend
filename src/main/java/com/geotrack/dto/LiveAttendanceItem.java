package com.geotrack.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveAttendanceItem {
    private String logId;
    private EmployeeDto employee;
    private String office;
    private String checkinTime;
    private String checkoutTime;
    private String status;
    private Double distance;
    private Long duration;
    
    @Data
    @Builder
    public static class EmployeeDto {
        private String id;
        private String name;
        private String email;
        private String employeeCode;
        private DepartmentDto department;
    }
    
    @Data
    @Builder
    public static class DepartmentDto {
        private String name;
    }
}
