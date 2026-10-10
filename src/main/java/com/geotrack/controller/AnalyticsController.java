package com.geotrack.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAnalytics(@RequestParam(required = false) String type) {
        String csvContent = "id,name,date,status\n1,Demo User,2026-10-10,Present\n";
        byte[] csvBytes = csvContent.getBytes();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "export.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}
