package com.railease.service.impl;

import com.railease.constants.TicketStatus;
import com.railease.dto.CancellationRuleDTO;
import com.railease.entity.CancellationRule;
import com.railease.entity.MealOrder;
import com.railease.entity.Ticket;
import com.railease.repository.CancellationRuleRepository;
import com.railease.repository.MealOrderRepository;
import com.railease.repository.TicketRepository;
import com.railease.service.CancellationService;
import com.railease.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CancellationServiceImpl implements CancellationService {

    private final TicketRepository ticketRepository;
    private final MealOrderRepository mealOrderRepository;
    private final CancellationRuleRepository cancellationRuleRepository;
    private final EmailService emailService;

    @Override
    public List<Ticket> getTicketCancellationRequests() {
        return ticketRepository.findCancellationRequests();
    }

    @Override
    public List<Ticket> getTicketCancellationHistory(String refundStatus, Long userId, String ticketId) {
        return ticketRepository.findAllCancellationHistory().stream()
                .filter(ticket -> refundStatus == null || refundStatus.isBlank()
                        || refundStatus.equalsIgnoreCase(ticket.getRefundStatus()))
                .filter(ticket -> userId == null || ticket.getUser().getUserId().equals(userId))
                .filter(ticket -> ticketId == null || ticketId.isBlank()
                        || ticket.getTicketId().toLowerCase().contains(ticketId.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MealOrder> getMealCancellationRequests() {
        return mealOrderRepository.findByRefundStatus("PENDING");
    }

    @Override
    public Map<String, Object> getAllCancellationRequests() {
        Map<String, Object> result = new HashMap<>();
        result.put("ticketRequests", getTicketCancellationRequests());
        result.put("mealRequests", getMealCancellationRequests());
        result.put("totalRequests", getTicketCancellationRequests().size() + getMealCancellationRequests().size());
        return result;
    }

    @Override
    public Ticket approveTicketCancellation(String ticketId) {
        Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        if (!"PENDING".equalsIgnoreCase(ticket.getRefundStatus())) {
            throw new RuntimeException("Only pending cancellation requests can be approved.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = ticket.getJourneyDate()
                .atTime(ticket.getTrain().getDepartureTime() != null ? ticket.getTrain().getDepartureTime() : LocalTime.MIDNIGHT);
        Double refundPercentage = calculateRefundPercentage(departureTime, now);
        Double refundAmount = ticket.getTotalFare() * (refundPercentage / 100);

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setBookingStatus("CANCELLED");
        ticket.setCancellationDate(now);
        ticket.setRefundStatus("PROCESSING");
        ticket.setRefundAmount(refundAmount);
        ticket.setRefundPercentage(refundPercentage);
        ticket.setCancellationCharges(ticket.getTotalFare() - refundAmount);
        ticket.setCancellationDecisionDate(now);
        ticket.setAdminRemarks("Cancellation approved");

        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket completeTicketRefund(String ticketId, String transactionId) {
        Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        if (!"PROCESSING".equalsIgnoreCase(ticket.getRefundStatus())) {
            throw new RuntimeException("Only processing refunds can be completed.");
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setRefundStatus("COMPLETED");
        ticket.setRefundDate(now);
        ticket.setRefundProcessedDate(now);
        ticket.setRefundTransactionId((transactionId == null || transactionId.isBlank())
                ? "RFN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : transactionId.trim());
        ticket.setAdminRemarks("Refund completed");

        Ticket updatedTicket = ticketRepository.save(ticket);
        notifyRefundProcessed(updatedTicket);
        return updatedTicket;
    }

    @Override
    public Ticket rejectTicketCancellation(String ticketId, String reason) {
        Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        ticket.setBookingStatus("CONFIRMED");
        ticket.setRefundStatus("DECLINED");
        ticket.setCancellationDecisionDate(LocalDateTime.now());
        ticket.setAdminRemarks(reason);

        return ticketRepository.save(ticket);
    }

    @Override
    public MealOrder approveMealCancellation(Long orderId, Double refundPercentage) {
        MealOrder order = mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Meal order not found with id: " + orderId));

        Double refundAmount = order.getTotalPrice() * (refundPercentage / 100);
        order.setDeliveryStatus("CANCELLED");
        order.setRefundStatus("APPROVED");
        order.setRefundAmount(refundAmount);
        order.setCancellationDate(LocalDateTime.now());
        return mealOrderRepository.save(order);
    }

    @Override
    public MealOrder rejectMealCancellation(Long orderId, String reason) {
        MealOrder order = mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Meal order not found with id: " + orderId));

        order.setRefundStatus("REJECTED");
        order.setCancellationReason(reason);
        return mealOrderRepository.save(order);
    }

    @Override
    public Double calculateTicketRefund(Double ticketPrice, LocalDateTime departureTime, LocalDateTime cancellationTime) {
        Double refundPercentage = calculateRefundPercentage(departureTime, cancellationTime);
        return ticketPrice * (refundPercentage / 100);
    }

    @Override
    public Double calculateRefundPercentage(LocalDateTime departureTime, LocalDateTime cancellationTime) {
        return findApplicableRule(departureTime, cancellationTime)
                .map(CancellationRule::getRefundPercentage)
                .orElse(0.0);
    }

    @Override
    public Optional<CancellationRule> findApplicableRule(LocalDateTime departureTime, LocalDateTime cancellationTime) {
        long hoursBeforeDeparture = ChronoUnit.HOURS.between(cancellationTime, departureTime);
        return ensureDefaultRules().stream()
                .filter(CancellationRule::getIsActive)
                .filter(rule -> hoursBeforeDeparture >= rule.getMinHoursBeforeDeparture())
                .filter(rule -> rule.getMaxHoursBeforeDeparture() == null
                        || hoursBeforeDeparture < rule.getMaxHoursBeforeDeparture())
                .findFirst();
    }

    @Override
    public List<CancellationRule> getCancellationRules() {
        return cancellationRuleRepository.findAllOrdered();
    }

    @Override
    public CancellationRule saveCancellationRule(CancellationRuleDTO ruleDTO) {
        if (ruleDTO.getMaxHoursBeforeDeparture() != null
                && ruleDTO.getMaxHoursBeforeDeparture() <= ruleDTO.getMinHoursBeforeDeparture()) {
            throw new RuntimeException("Maximum hours must be greater than minimum hours.");
        }

        CancellationRule rule = ruleDTO.getId() != null
                ? cancellationRuleRepository.findById(ruleDTO.getId())
                .orElseThrow(() -> new RuntimeException("Cancellation rule not found"))
                : new CancellationRule();

        rule.setMinHoursBeforeDeparture(ruleDTO.getMinHoursBeforeDeparture());
        rule.setMaxHoursBeforeDeparture(ruleDTO.getMaxHoursBeforeDeparture());
        rule.setRefundPercentage(ruleDTO.getRefundPercentage());
        rule.setDescription(ruleDTO.getDescription());
        rule.setIsActive(ruleDTO.getIsActive() == null || ruleDTO.getIsActive());
        return cancellationRuleRepository.save(rule);
    }

    @Override
    public void deleteCancellationRule(Long ruleId) {
        cancellationRuleRepository.deleteById(ruleId);
    }

    @Override
    public Map<String, Object> getCancellationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Ticket> pendingTicketRefunds = ticketRepository.findByRefundStatus("PENDING");
        List<Ticket> processingTicketRefunds = ticketRepository.findByRefundStatus("PROCESSING");
        List<Ticket> completedTicketRefunds = ticketRepository.findByRefundStatus("COMPLETED");
        List<MealOrder> pendingMealRefunds = mealOrderRepository.findByRefundStatus("PENDING");
        List<Ticket> ticketHistory = getTicketCancellationHistory(null, null, null);

        stats.put("pendingTicketRefunds", pendingTicketRefunds.size());
        stats.put("processingTicketRefunds", processingTicketRefunds.size());
        stats.put("completedTicketRefunds", completedTicketRefunds.size());
        stats.put("pendingMealRefunds", pendingMealRefunds.size());
        stats.put("totalPending", pendingTicketRefunds.size() + pendingMealRefunds.size());
        stats.put("totalTicketRequests", ticketHistory.size());

        double totalTicketRefundAmount = ticketHistory.stream()
                .map(Ticket::getRefundAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalMealRefundAmount = pendingMealRefunds.stream()
                .map(MealOrder::getRefundAmount)
                .filter(amount -> amount != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        stats.put("totalRefundAmount", totalTicketRefundAmount + totalMealRefundAmount);
        return stats;
    }

    @Override
    public Ticket processBulkCancellation(List<Long> ticketIds, String reason) {
        for (Long ticketId : ticketIds) {
            Ticket ticket = ticketRepository.findById(String.valueOf(ticketId))
                    .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
            LocalDateTime departureTime = ticket.getJourneyDate()
                    .atTime(ticket.getTrain().getDepartureTime() != null ? ticket.getTrain().getDepartureTime() : LocalTime.MIDNIGHT);
            Double refundPercentage = calculateRefundPercentage(departureTime, LocalDateTime.now());
            Double refundAmount = ticket.getTotalFare() * (refundPercentage / 100);

            ticket.setTicketStatus(TicketStatus.CANCELLED);
            ticket.setBookingStatus("CANCELLED");
            ticket.setRefundStatus("PROCESSING");
            ticket.setRefundAmount(refundAmount);
            ticket.setRefundPercentage(refundPercentage);
            ticket.setCancellationReason(reason);
            ticket.setCancellationDate(LocalDateTime.now());
            ticket.setCancellationDecisionDate(LocalDateTime.now());
            ticketRepository.save(ticket);
        }
        return null;
    }

    private List<CancellationRule> ensureDefaultRules() {
        List<CancellationRule> existingRules = cancellationRuleRepository.findActiveRules();
        if (!existingRules.isEmpty()) {
            return existingRules;
        }

        List<CancellationRule> defaults = List.of(
                CancellationRule.builder()
                        .minHoursBeforeDeparture(24)
                        .maxHoursBeforeDeparture(null)
                        .refundPercentage(100.0)
                        .description("24+ hours before departure")
                        .isActive(true)
                        .build(),
                CancellationRule.builder()
                        .minHoursBeforeDeparture(12)
                        .maxHoursBeforeDeparture(24)
                        .refundPercentage(50.0)
                        .description("12-24 hours before departure")
                        .isActive(true)
                        .build(),
                CancellationRule.builder()
                        .minHoursBeforeDeparture(0)
                        .maxHoursBeforeDeparture(12)
                        .refundPercentage(0.0)
                        .description("Less than 12 hours before departure")
                        .isActive(true)
                        .build()
        );
        return cancellationRuleRepository.saveAll(defaults);
    }

    private void notifyRefundProcessed(Ticket ticket) {
        try {
            emailService.sendCancellationEmail(ticket.getUser(), ticket.getTicketId(), ticket.getRefundAmount());
        } catch (Exception e) {
            log.warn("Failed to send refund completion email for {}: {}", ticket.getTicketId(), e.getMessage());
        }
    }
}
