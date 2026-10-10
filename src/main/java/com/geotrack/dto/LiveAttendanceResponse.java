package com.geotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAttendanceResponse {
    private List<LiveAttendanceItem> liveAttendance;
    private int total;
}
