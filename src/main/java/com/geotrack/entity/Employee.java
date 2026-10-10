package com.geotrack.entity;

import com.geotrack.entity.enums.EmployeeStatus;
import com.geotrack.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"})
}, indexes = {
        @Index(columnList = "organization_id"),
        @Index(columnList = "department_id")
})
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Office office;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EMPLOYEE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE)
    private java.util.List<AttendanceLog> attendanceLogs;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE)
    private java.util.List<WorkLog> workLogs;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE)
    private java.util.List<LeaveRequest> leaveRequests;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE)
    private java.util.List<Notification> notifications;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE)
    private java.util.List<RefreshToken> refreshTokens;
}
