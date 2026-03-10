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
public class CancellationResponseDTO {
    private String ticketId;
    private String status;
    private String message;
    private Double refundAmount;
    private Double cancellationCharges;
    private LocalDateTime cancellationTime;
    private String refundStatus;
    private String estimatedRefundDate;
    private String transactionId;
    private Boolean success;
    private String redirectUrl;
    private LocalDateTime actualCompletionDate;  // Add this field
}