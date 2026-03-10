package com.railease.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class RefundSlabDTO {
    private Integer fromHours;
    private Integer toHours;
    private Double refundPercentage;
    private String description;

    // Constructor with all fields
    public RefundSlabDTO(Integer fromHours, Integer toHours, Double refundPercentage, String description) {
        this.fromHours = fromHours;
        this.toHours = toHours;
        this.refundPercentage = refundPercentage;
        this.description = description;
    }
}