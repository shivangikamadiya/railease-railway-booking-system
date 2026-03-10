package com.railease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusDTO {
    private String ticketId;
    private String ticketNumber;
    private String trainName;
    private String passengerName;
    private LocalDateTime cancellationDate;
    private Double originalFare;
    private Double cancellationCharges;
    private Double refundAmount;
    private String refundStatus;
    private String refundStatusMessage;
    private LocalDateTime estimatedCompletionDate;
    private LocalDateTime actualCompletionDate;
    private String refundTransactionId;
    private String paymentMethod;
    private String refundMethod;
    private List<RefundTimelineDTO> timeline;
}