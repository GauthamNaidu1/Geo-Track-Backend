package com.geotrack.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmployeeListResponse {
    private List<EmployeeResponse> employees;
    private Pagination pagination;

    @Data
    @Builder
    public static class Pagination {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
    }
}
