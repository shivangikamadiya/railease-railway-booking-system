package com.railease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyDTO {
    private List<RefundSlabDTO> refundSlabs;
    private Map<String, String> terms;
    private Integer minimumHoursForCancellation;
    private Double maximumRefundPercentage;
    private String policyEffectiveDate;
    private String policyVersion;
}