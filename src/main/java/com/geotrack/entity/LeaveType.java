package com.geotrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "leave_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "name"})
})
public class LeaveType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "days_allowed")
    private Integer daysAllowed = 0;

    @Column(name = "requires_approval")
    private Boolean requiresApproval = true;
}
