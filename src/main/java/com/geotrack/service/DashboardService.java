package com.geotrack.service;

import com.geotrack.dto.DashboardStats;
import com.geotrack.dto.LiveAttendanceItem;
import com.geotrack.dto.LiveAttendanceResponse;
import com.geotrack.entity.AttendanceLog;
import com.geotrack.entity.Employee;
import com.geotrack.entity.enums.LeaveStatus;
import com.geotrack.repository.AttendanceLogRepository;
import com.geotrack.repository.EmployeeRepository;
import com.geotrack.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional(readOnly = true)
    public DashboardStats getStats(UUID organizationId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long totalEmployees = employeeRepository.countByOrganizationId(organizationId);
        long checkedIn = attendanceLogRepository.countByEmployeeOrganizationIdAndCheckinTimeBetweenAndCheckoutTimeIsNull(organizationId, startOfDay, endOfDay);
        long checkedOut = attendanceLogRepository.countByEmployeeOrganizationIdAndCheckinTimeBetweenAndCheckoutTimeIsNotNull(organizationId, startOfDay, endOfDay);
        long onLeave = leaveRequestRepository.countByOrganizationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(organizationId, LeaveStatus.APPROVED, today, today);
        long absent = totalEmployees - checkedIn - checkedOut - onLeave;

        return DashboardStats.builder()
                .date(startOfDay.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
                .totalEmployees(totalEmployees)
                .checkedIn(checkedIn)
                .checkedOut(checkedOut)
                .onLeave(onLeave)
                .absent(absent)
                .build();
    }

    @Transactional(readOnly = true)
    public LiveAttendanceResponse getLiveAttendance(UUID organizationId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<AttendanceLog> logs = attendanceLogRepository.findByEmployeeOrganizationIdAndCheckinTimeBetweenOrderByCheckinTimeDesc(organizationId, startOfDay, endOfDay);

        List<LiveAttendanceItem> items = logs.stream().map(log -> {
            Employee emp = log.getEmployee();
            String name = emp.getFirstName() + " " + emp.getLastName();
            
            LiveAttendanceItem.EmployeeDto empDto = LiveAttendanceItem.EmployeeDto.builder()
                    .id(emp.getId().toString())
                    .name(name)
                    .email(emp.getEmail())
                    .employeeCode("") // Omitted as it's not in Employee entity
                    .department(emp.getDepartment() != null ? LiveAttendanceItem.DepartmentDto.builder().name(emp.getDepartment().getName()).build() : null)
                    .build();

            long duration = 0;
            if (log.getCheckoutTime() != null) {
                duration = Duration.between(log.getCheckinTime(), log.getCheckoutTime()).toMinutes();
            } else {
                duration = Duration.between(log.getCheckinTime(), LocalDateTime.now()).toMinutes();
            }

            return LiveAttendanceItem.builder()
                    .logId(log.getId().toString())
                    .employee(empDto)
                    .office(log.getOffice() != null ? log.getOffice().getName() : "")
                    .checkinTime(log.getCheckinTime().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
                    .checkoutTime(log.getCheckoutTime() != null ? log.getCheckoutTime().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT) : null)
                    .status(log.getCheckoutTime() != null ? "checked-out" : "checked-in")
                    .distance(log.getDistanceFromOffice())
                    .duration(duration)
                    .build();
        }).collect(Collectors.toList());

        return LiveAttendanceResponse.builder()
                .liveAttendance(items)
                .total(items.size())
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getTeamStatus(UUID organizationId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Employee> employees = employeeRepository.findByOrganizationId(organizationId);

        List<AttendanceLog> allLogs = attendanceLogRepository.findByEmployeeOrganizationIdAndCheckinTimeBetweenOrderByCheckinTimeDesc(organizationId, startOfDay, endOfDay);
        java.util.Map<UUID, AttendanceLog> logByEmployee = new java.util.HashMap<>();
        for (AttendanceLog log : allLogs) {
            logByEmployee.putIfAbsent(log.getEmployee().getId(), log);
        }

        List<com.geotrack.entity.LeaveRequest> allLeaves = leaveRequestRepository.findByOrganizationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                organizationId, LeaveStatus.APPROVED, today, today);
        java.util.Map<UUID, com.geotrack.entity.LeaveRequest> leaveByEmployee = new java.util.HashMap<>();
        for (com.geotrack.entity.LeaveRequest leave : allLeaves) {
            leaveByEmployee.put(leave.getEmployee().getId(), leave);
        }

        List<java.util.Map<String, Object>> teamStatus = employees.stream().map(emp -> {
            String status = "absent";
            java.util.Map<String, Object> attendanceData = null;
            java.util.Map<String, Object> leaveData = null;

            if (leaveByEmployee.containsKey(emp.getId())) {
                status = "on-leave";
                leaveData = java.util.Map.of("type", leaveByEmployee.get(emp.getId()).getLeaveType().getName());
            } else if (logByEmployee.containsKey(emp.getId())) {
                AttendanceLog log = logByEmployee.get(emp.getId());
                status = log.getCheckoutTime() != null ? "checked-out" : "checked-in";
                attendanceData = new java.util.HashMap<>();
                attendanceData.put("checkinTime", log.getCheckinTime().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
                if (log.getCheckoutTime() != null) {
                    attendanceData.put("checkoutTime", log.getCheckoutTime().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
                }
                if (log.getOffice() != null) {
                    attendanceData.put("office", log.getOffice().getName());
                }
            }

            java.util.Map<String, Object> member = new java.util.HashMap<>();
            member.put("id", emp.getId().toString());
            member.put("name", emp.getFirstName() + " " + emp.getLastName());
            member.put("employeeCode", "");
            member.put("status", status);
            if (attendanceData != null) member.put("attendance", attendanceData);
            if (leaveData != null) member.put("leave", leaveData);
            
            return member;
        }).collect(Collectors.toList());

        return java.util.Map.of("teamStatus", teamStatus);
    }
}
