package com.railease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEstimateDTO {
    private String ticketId;
    private Double originalFare;
    private Double refundAmount;
    private Double cancellationCharges;
    private Double refundPercentage;
    private String refundPolicy;
    private LocalDateTime estimatedRefundDate;
    private Boolean isEligible;
    private String eligibilityMessage;
    private String paymentMethod;
    private String lastFourDigits;
}