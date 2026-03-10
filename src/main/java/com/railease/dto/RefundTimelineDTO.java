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
public class RefundTimelineDTO {
    private String stage;
    private String status;
    private LocalDateTime timestamp;
    private String message;
    private Boolean isCompleted;
}