package com.railease.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "min_hours_before_departure", nullable = false)
    private Integer minHoursBeforeDeparture;

    @Column(name = "max_hours_before_departure")
    private Integer maxHoursBeforeDeparture;

    @Column(name = "refund_percentage", nullable = false)
    private Double refundPercentage;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
