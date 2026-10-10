package com.geotrack.controller;

import com.geotrack.entity.LeaveRequest;
import com.geotrack.entity.LeaveType;
import com.geotrack.entity.Employee;
import com.geotrack.repository.LeaveRequestRepository;
import com.geotrack.repository.LeaveTypeRepository;
import com.geotrack.repository.EmployeeRepository;
import com.geotrack.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/requests")
    public ResponseEntity<?> getLeaves(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();

        Page<LeaveRequest> leavePage = leaveRequestRepository.findByOrganizationId(
                user.getEmployee().getOrganization().getId(), PageRequest.of(page - 1, limit));

        List<Map<String, Object>> leaveRequests = leavePage.getContent().stream().map(leave -> Map.of(
                "id", leave.getId(),
                "startDate", leave.getStartDate(),
                "endDate", leave.getEndDate(),
                "reason", leave.getReason() == null ? "" : leave.getReason(),
                "status", leave.getStatus().name().toLowerCase(),
                "leaveType", Map.of("name", leave.getLeaveType().getName()),
                "employee", Map.of(
                        "name", leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName(),
                        "email", leave.getEmployee().getEmail(),
                        "employeeCode", ""
                ),
                "createdAt", leave.getCreatedAt()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "leaveRequests", leaveRequests,
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", leavePage.getTotalElements(),
                        "totalPages", leavePage.getTotalPages()
                )
        ));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<?> getLeave(@PathVariable UUID id) {
        return leaveRequestRepository.findById(id).map(leave -> {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", leave.getId().toString());
            data.put("startDate", leave.getStartDate().toString());
            data.put("endDate", leave.getEndDate().toString());
            data.put("reason", leave.getReason() == null ? "" : leave.getReason());
            data.put("status", leave.getStatus().name().toLowerCase());
            data.put("leaveType", Map.of(
                    "id", leave.getLeaveType().getId().toString(),
                    "name", leave.getLeaveType().getName()
            ));
            data.put("employee", Map.of(
                    "id", leave.getEmployee().getId().toString(),
                    "name", leave.getEmployee().getFirstName() + (leave.getEmployee().getLastName() != null ? " " + leave.getEmployee().getLastName() : ""),
                    "email", leave.getEmployee().getEmail(),
                    "employeeCode", "EMP-" + leave.getEmployee().getId().toString().substring(0, 6).toUpperCase()
            ));
            data.put("createdAt", leave.getCreatedAt() != null ? leave.getCreatedAt().toString() : "");

            Map<String, Object> response = new java.util.HashMap<>(data);
            response.put("leaveRequest", data);
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/requests/{id}/approve", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> approveLeave(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails user) {
        return leaveRequestRepository.findById(id).map(leave -> {
            leave.setStatus(com.geotrack.entity.enums.LeaveStatus.APPROVED);
            if (user != null && user.getEmployee() != null) {
                employeeRepository.findById(user.getEmployee().getId()).ifPresent(leave::setDecisionBy);
            }
            leave.setDecisionAt(java.time.LocalDateTime.now());
            leaveRequestRepository.save(leave);
            return ResponseEntity.ok(Map.of("message", "Leave approved successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/requests/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> rejectLeave(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> payload, @AuthenticationPrincipal CustomUserDetails user) {
        return leaveRequestRepository.findById(id).map(leave -> {
            leave.setStatus(com.geotrack.entity.enums.LeaveStatus.REJECTED);
            if (user != null && user.getEmployee() != null) {
                employeeRepository.findById(user.getEmployee().getId()).ifPresent(leave::setDecisionBy);
            }
            leave.setDecisionAt(java.time.LocalDateTime.now());
            leaveRequestRepository.save(leave);
            return ResponseEntity.ok(Map.of("message", "Leave rejected successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getLeaveBalance(@PathVariable UUID userId) {
        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) return ResponseEntity.notFound().build();

        List<LeaveType> leaveTypes = leaveTypeRepository.findByOrganizationId(emp.getOrganization().getId());
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findAll().stream()
                .filter(l -> l.getEmployee().getId().equals(userId) && l.getStatus() == com.geotrack.entity.enums.LeaveStatus.APPROVED)
                .collect(Collectors.toList());

        List<Map<String, Object>> balances = leaveTypes.stream().map(type -> {
            long usedDays = approvedLeaves.stream()
                    .filter(l -> l.getLeaveType().getId().equals(type.getId()))
                    .mapToLong(l -> ChronoUnit.DAYS.between(l.getStartDate(), l.getEndDate()) + 1)
                    .sum();

            int totalDays = type.getDaysAllowed() != null ? type.getDaysAllowed() : 0;
            long availableDays = Math.max(0, totalDays - usedDays);

            return Map.<String, Object>of(
                    "leaveTypeId", type.getId(),
                    "name", type.getName(),
                    "totalDays", totalDays,
                    "usedDays", usedDays,
                    "availableDays", availableDays
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("balances", balances));
    }

    @PostMapping({"/request", "/requests"})
    public ResponseEntity<?> submitLeaveRequest(@RequestBody Map<String, String> payload, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null || user.getEmployee() == null) return ResponseEntity.status(401).build();
        
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(user.getEmployee());
        request.setOrganization(user.getEmployee().getOrganization());
        request.setStatus(com.geotrack.entity.enums.LeaveStatus.PENDING);
        request.setStartDate(LocalDate.parse(payload.get("startDate")));
        request.setEndDate(LocalDate.parse(payload.get("endDate")));
        request.setReason(payload.get("reason"));

        LeaveType type = new LeaveType();
        type.setId(UUID.fromString(payload.get("leaveTypeId")));
        request.setLeaveType(type);

        leaveRequestRepository.save(request);

        return ResponseEntity.ok(Map.of("message", "Leave request submitted successfully!"));
    }
}
