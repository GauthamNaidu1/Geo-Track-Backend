package com.geotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private String date;
    private long totalEmployees;
    private long checkedIn;
    private long checkedOut;
    private long onLeave;
    private long absent;
}
