package com.railease.service.impl;

import com.railease.entity.Ticket;
import com.railease.entity.MealOrder;
import com.railease.repository.TicketRepository;
import com.railease.repository.MealOrderRepository;
import com.railease.service.CancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CancellationServiceImpl implements CancellationService {

    private final TicketRepository ticketRepository;
    private final MealOrderRepository mealOrderRepository;

    @Override
    public List<Ticket> getTicketCancellationRequests() {
        log.info("Fetching ticket cancellation requests");
        return ticketRepository.findCancellationRequests();
    }

    @Override
    public List<MealOrder> getMealCancellationRequests() {
        log.info("Fetching meal cancellation requests");
        return mealOrderRepository.findByRefundStatus("PENDING");
    }

    @Override
    public Map<String, Object> getAllCancellationRequests() {
        Map<String, Object> result = new HashMap<>();
        result.put("ticketRequests", getTicketCancellationRequests());
        result.put("mealRequests", getMealCancellationRequests());
        result.put("totalRequests",
                getTicketCancellationRequests().size() +
                        getMealCancellationRequests().size());
        return result;
    }

    @Override
    public Ticket approveTicketCancellation(Long ticketId, Double refundPercentage) {
        log.info("Approving ticket cancellation for ticket: {} with refund: {}%", ticketId, refundPercentage);

        // FIXED: ticketId is Long, but repository might expect String
        Ticket ticket = ticketRepository.findById(String.valueOf(ticketId))  // Convert Long to String
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        LocalDateTime departureTime = ticket.getJourneyDate()
                .atTime(ticket.getTrain().getDepartureTime());

        Double refundAmount = ticket.getTotalFare() * (refundPercentage / 100);

        ticket.setTicketStatus(com.railease.constants.TicketStatus.CANCELLED);
        ticket.setBookingStatus("CANCELLED");
        ticket.setRefundStatus("APPROVED");
        ticket.setRefundAmount(refundAmount);
        ticket.setRefundPercentage(refundPercentage);
        ticket.setRefundProcessedDate(LocalDateTime.now());

        // Update seat availability
        Integer availableSeats = ticket.getTrain().getAvailableSeats();
        ticket.getTrain().setAvailableSeats(availableSeats + ticket.getNumberOfSeats());

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket cancellation approved for ticket: {}, refund amount: ₹{}", ticketId, refundAmount);
        return updatedTicket;
    }

    @Override
    public Ticket rejectTicketCancellation(Long ticketId, String reason) {
        log.info("Rejecting ticket cancellation for ticket: {}", ticketId);

        // FIXED: ticketId is Long, but repository might expect String
        Ticket ticket = ticketRepository.findById(String.valueOf(ticketId))
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        ticket.setRefundStatus("REJECTED");
        ticket.setCancellationReason(reason);

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket cancellation rejected for ticket: {}", ticketId);
        return updatedTicket;
    }

    @Override
    public MealOrder approveMealCancellation(Long orderId, Double refundPercentage) {
        log.info("Approving meal cancellation for order: {} with refund: {}%", orderId, refundPercentage);

        MealOrder order = mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Meal order not found with id: " + orderId));

        Double refundAmount = order.getTotalPrice() * (refundPercentage / 100);

        order.setDeliveryStatus("CANCELLED");
        order.setRefundStatus("APPROVED");
        order.setRefundAmount(refundAmount);
        order.setCancellationDate(LocalDateTime.now());

        MealOrder updatedOrder = mealOrderRepository.save(order);
        log.info("Meal cancellation approved for order: {}, refund amount: ₹{}", orderId, refundAmount);
        return updatedOrder;
    }

    @Override
    public MealOrder rejectMealCancellation(Long orderId, String reason) {
        log.info("Rejecting meal cancellation for order: {}", orderId);

        MealOrder order = mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Meal order not found with id: " + orderId));

        order.setRefundStatus("REJECTED");
        order.setCancellationReason(reason);

        MealOrder updatedOrder = mealOrderRepository.save(order);
        log.info("Meal cancellation rejected for order: {}", orderId);
        return updatedOrder;
    }

    @Override
    public Double calculateTicketRefund(Double ticketPrice, LocalDateTime departureTime, LocalDateTime cancellationTime) {
        Double refundPercentage = calculateRefundPercentage(departureTime, cancellationTime);
        return ticketPrice * (refundPercentage / 100);
    }

    @Override
    public Double calculateRefundPercentage(LocalDateTime departureTime, LocalDateTime cancellationTime) {
        long hoursBeforeDeparture = ChronoUnit.HOURS.between(cancellationTime, departureTime);

        log.info("Calculating refund for cancellation {} hours before departure", hoursBeforeDeparture);

        if (hoursBeforeDeparture > 48) {
            return 90.0;
        } else if (hoursBeforeDeparture > 24) {
            return 70.0;
        } else if (hoursBeforeDeparture > 12) {
            return 50.0;
        } else if (hoursBeforeDeparture > 0) {
            return 20.0;
        } else {
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getCancellationStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Ticket> pendingTicketRefunds = ticketRepository.findByRefundStatus("PENDING");
        List<MealOrder> pendingMealRefunds = mealOrderRepository.findByRefundStatus("PENDING");

        stats.put("pendingTicketRefunds", pendingTicketRefunds.size());
        stats.put("pendingMealRefunds", pendingMealRefunds.size());
        stats.put("totalPending", pendingTicketRefunds.size() + pendingMealRefunds.size());

        Double totalTicketRefundAmount = pendingTicketRefunds.stream()
                .mapToDouble(Ticket::getRefundAmount)
                .sum();

        Double totalMealRefundAmount = pendingMealRefunds.stream()
                .mapToDouble(MealOrder::getRefundAmount)
                .sum();

        stats.put("totalRefundAmount", totalTicketRefundAmount + totalMealRefundAmount);

        return stats;
    }

    @Override
    public Ticket processBulkCancellation(List<Long> ticketIds, String reason) {
        log.info("Processing bulk cancellation for {} tickets", ticketIds.size());

        for (Long ticketId : ticketIds) {
            // FIXED: Convert Long to String for repository lookup
            Ticket ticket = ticketRepository.findById(String.valueOf(ticketId))
                    .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

            LocalDateTime departureTime = ticket.getJourneyDate()
                    .atTime(ticket.getTrain().getDepartureTime());

            Double refundPercentage = calculateRefundPercentage(departureTime, LocalDateTime.now());
            Double refundAmount = ticket.getTotalFare() * (refundPercentage / 100);

            ticket.setTicketStatus(com.railease.constants.TicketStatus.CANCELLED);
            ticket.setBookingStatus("CANCELLED");
            ticket.setRefundStatus("APPROVED");
            ticket.setRefundAmount(refundAmount);
            ticket.setRefundPercentage(refundPercentage);
            ticket.setCancellationReason(reason);
            ticket.setRefundProcessedDate(LocalDateTime.now());

            ticketRepository.save(ticket);
        }

        log.info("Bulk cancellation completed for {} tickets", ticketIds.size());
        return null;
    }
}