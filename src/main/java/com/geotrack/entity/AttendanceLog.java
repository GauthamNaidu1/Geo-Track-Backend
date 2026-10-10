package com.geotrack.entity;

import com.geotrack.entity.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "attendance_logs", indexes = {
        @Index(columnList = "employee_id"),
        @Index(columnList = "organization_id"),
        @Index(columnList = "office_id")
})
public class AttendanceLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Office office;

    @Column(name = "checkin_time", nullable = false)
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    @Column(name = "checkin_latitude")
    private Double checkinLatitude;

    @Column(name = "checkin_longitude")
    private Double checkinLongitude;

    @Column(name = "checkout_latitude")
    private Double checkoutLatitude;

    @Column(name = "checkout_longitude")
    private Double checkoutLongitude;

    @Column(name = "distance_from_office")
    private Double distanceFromOffice;

    @Column(name = "hours_worked")
    private Double hoursWorked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;
}
