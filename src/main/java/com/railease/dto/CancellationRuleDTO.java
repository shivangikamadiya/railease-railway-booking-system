package com.railease.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class CancellationRuleDTO {

    private Long id;

    @NotNull(message = "Minimum hours is required")
    @Min(value = 0, message = "Minimum hours cannot be negative")
    private Integer minHoursBeforeDeparture;

    @Min(value = 0, message = "Maximum hours cannot be negative")
    private Integer maxHoursBeforeDeparture;

    @NotNull(message = "Refund percentage is required")
    @Min(value = 0, message = "Refund percentage cannot be negative")
    @Max(value = 100, message = "Refund percentage cannot exceed 100")
    private Double refundPercentage;

    private String description;

    private Boolean isActive = true;
}
