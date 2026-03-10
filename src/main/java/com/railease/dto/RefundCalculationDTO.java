package com.railease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCalculationDTO {
    private Double originalFare;
    private Double refundAmount;
    private Double cancellationCharges;
    private Double refundPercentage;
    private String appliedPolicy;
    private Integer hoursBeforeDeparture;
    private Boolean isEligible;
    private String message;
}