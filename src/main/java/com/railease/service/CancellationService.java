package com.railease.service;

import com.railease.dto.CancellationRequestDTO;
import com.railease.entity.Ticket;
import com.railease.entity.MealOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CancellationService {

    List<Ticket> getTicketCancellationRequests();

    List<MealOrder> getMealCancellationRequests();

    Map<String, Object> getAllCancellationRequests();

    Ticket approveTicketCancellation(Long ticketId, Double refundPercentage);

    Ticket rejectTicketCancellation(Long ticketId, String reason);

    MealOrder approveMealCancellation(Long orderId, Double refundPercentage);

    MealOrder rejectMealCancellation(Long orderId, String reason);

    Double calculateTicketRefund(Double ticketPrice, LocalDateTime departureTime, LocalDateTime cancellationTime);

    Double calculateRefundPercentage(LocalDateTime departureTime, LocalDateTime cancellationTime);

    Map<String, Object> getCancellationStatistics();

    Ticket processBulkCancellation(List<Long> ticketIds, String reason);
}