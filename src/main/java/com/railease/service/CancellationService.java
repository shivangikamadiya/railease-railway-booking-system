package com.railease.service;

import com.railease.dto.CancellationRuleDTO;
import com.railease.dto.CancellationRequestDTO;
import com.railease.entity.CancellationRule;
import com.railease.entity.Ticket;
import com.railease.entity.MealOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CancellationService {

    List<Ticket> getTicketCancellationRequests();

    List<Ticket> getTicketCancellationHistory(String refundStatus, Long userId, String ticketId);

    List<MealOrder> getMealCancellationRequests();

    Map<String, Object> getAllCancellationRequests();

    Ticket approveTicketCancellation(String ticketId);

    Ticket completeTicketRefund(String ticketId, String transactionId);

    Ticket rejectTicketCancellation(String ticketId, String reason);

    MealOrder approveMealCancellation(Long orderId, Double refundPercentage);

    MealOrder rejectMealCancellation(Long orderId, String reason);

    Double calculateTicketRefund(Double ticketPrice, LocalDateTime departureTime, LocalDateTime cancellationTime);

    Double calculateRefundPercentage(LocalDateTime departureTime, LocalDateTime cancellationTime);

    Optional<CancellationRule> findApplicableRule(LocalDateTime departureTime, LocalDateTime cancellationTime);

    List<CancellationRule> getCancellationRules();

    CancellationRule saveCancellationRule(CancellationRuleDTO ruleDTO);

    void deleteCancellationRule(Long ruleId);

    Map<String, Object> getCancellationStatistics();

    Ticket processBulkCancellation(List<Long> ticketIds, String reason);
}
