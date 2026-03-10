package com.railease.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CancellationRequestDTO {

    @NotBlank(message = "Ticket ID is required")
    private String ticketId;

    @NotBlank(message = "Request type is required")
    private String requestType; // TICKET or MEAL

    @NotNull(message = "Approve/Reject decision is required")
    private Boolean approve;

    private Double customRefundPercentage;

    private String rejectionReason;

    // Add this field for cancellation reason
    private String reason;

    // Lombok @Data will generate getters and setters including getReason()
}