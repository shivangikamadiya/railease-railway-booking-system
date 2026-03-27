package com.railease.service.impl;

import com.railease.constants.TicketStatus;
import com.railease.dto.*;
import com.railease.entity.CancellationRule;
import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
import com.railease.repository.CancellationRuleRepository;
import com.railease.repository.TicketRepository;
import com.railease.repository.TrainRepository;
import com.railease.repository.UserRepository;
import com.railease.service.BookingService;
import com.railease.util.TicketIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final double GST_RATE = 0.05;

    private final TicketRepository ticketRepository;
    private final TrainRepository trainRepository;
    private final UserRepository userRepository;
    private final CancellationRuleRepository cancellationRuleRepository;
    private final TicketIdGenerator ticketIdGenerator;

    @Override
    public Ticket createTicket(Long userId, Integer trainNo, String passengerName,
                               Integer passengerAge, String passengerGender,
                               String classType, Integer numberOfSeats, LocalDate journeyDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Train train = trainRepository.findById(trainNo)
                .orElseThrow(() -> new RuntimeException("Train not found with number: " + trainNo));
        String normalizedClassType = normalizeClassType(classType);

        int availableSeats = getAvailableSeats(trainNo, normalizedClassType, journeyDate);
        if (availableSeats < numberOfSeats) {
            throw new RuntimeException("Only " + availableSeats + " seats available.");
        }

        double baseFare = resolveClassFare(train, normalizedClassType) * numberOfSeats;
        double totalFare = baseFare + (baseFare * GST_RATE);

        Ticket ticket = Ticket.builder()
                .ticketId(ticketIdGenerator.generateTicketId())
                .user(user)
                .train(train)
                .journeyDate(journeyDate)
                .sourceStation(Objects.requireNonNullElse(train.getSourceStation(), train.getSource()))
                .destinationStation(Objects.requireNonNullElse(train.getDestinationStation(), train.getDestination()))
                .passengerName(passengerName)
                .passengerAge(passengerAge)
                .passengerGender(passengerGender)
                .classType(normalizedClassType)
                .numberOfSeats(numberOfSeats)
                .totalFare(totalFare)
                .ticketStatus(TicketStatus.PENDING)
                .bookingStatus("PENDING")
                .paymentStatus("PENDING")
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created: {} for user: {}", savedTicket.getTicketId(), userId);
        return savedTicket;
    }

    @Override
    public Ticket confirmBooking(String ticketId, String paymentId, String paymentMethod) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setPaymentId(paymentId);
        ticket.setPaymentMethod(paymentMethod);
        ticket.setPaymentStatus("PAID");
        ticket.setBookingStatus("CONFIRMED");
        ticket.setTicketStatus(TicketStatus.CONFIRMED);
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketById(String ticketId) {
        return ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return ticketRepository.findByUserOrderByBookingDateDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getUserRecentBookings(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return ticketRepository.findRecentByUser(user, PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getUserActiveBookings(Long userId) {
        return ticketRepository.findActiveTicketsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getUserPastBookings(Long userId) {
        return ticketRepository.findPastTicketsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getUserCancelledBookings(Long userId) {
        return ticketRepository.findCancelledTicketsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getUserRefundedBookings(Long userId) {
        return ticketRepository.findTicketsWithRefundByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUserBookingCount(Long userId) {
        return ticketRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUserUpcomingJourneysCount(Long userId) {
        return ticketRepository.countUpcomingByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalSpentByUser(Long userId) {
        List<Ticket> tickets = getUserBookings(userId);
        return tickets.stream()
                .filter(ticket -> "PAID".equalsIgnoreCase(ticket.getPaymentStatus()))
                .mapToDouble(Ticket::getTotalFare)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableSeats(Integer trainNo, String classType, LocalDate journeyDate) {
        Train train = trainRepository.findById(trainNo)
                .orElseThrow(() -> new RuntimeException("Train not found with number: " + trainNo));
        String normalizedClassType = normalizeClassType(classType);

        int totalSeats = switch (normalizedClassType) {
            case "AC" -> train.getAcSeats() != null ? train.getAcSeats() : 0;
            case "SLEEPER" -> train.getSleeperSeats() != null ? train.getSleeperSeats() : 0;
            case "GENERAL" -> train.getGeneralSeats() != null ? train.getGeneralSeats() : 0;
            default -> train.getAvailableSeats() != null ? train.getAvailableSeats() : 0;
        };

        Long bookedSeats = ticketRepository.countBookedSeatsByTrainAndDateAndClass(trainNo, journeyDate, normalizedClassType);
        return Math.max(0, totalSeats - (bookedSeats != null ? bookedSeats.intValue() : 0));
    }

    @Override
    public CancellationResponseDTO initiateCancellation(String ticketId, Long userId, CancellationRequestDTO request) {
        return processCancellation(ticketId, userId, request.getReason());
    }

    @Override
    public CancellationResponseDTO processCancellation(String ticketId, Long userId, String reason) {
        Ticket ticket = getTicketById(ticketId);
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized cancellation request.");
        }
        if (!isTicketEligibleForCancellation(ticket)) {
            throw new RuntimeException("Only confirmed future tickets can be cancelled.");
        }
        if ("PENDING".equalsIgnoreCase(ticket.getRefundStatus())
                || "PROCESSING".equalsIgnoreCase(ticket.getRefundStatus())) {
            throw new RuntimeException("Cancellation request is already under review.");
        }

        RefundCalculationDTO calculation = buildRefundCalculation(
                ticket.getTotalFare(),
                getDepartureDateTime(ticket),
                LocalDateTime.now()
        );

        if (!Boolean.TRUE.equals(calculation.getIsEligible())) {
            throw new RuntimeException(calculation.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setBookingStatus("CANCELLATION_PENDING");
        ticket.setCancellationReason(reason);
        ticket.setRefundStatus("PENDING");
        ticket.setRefundAmount(calculation.getRefundAmount());
        ticket.setRefundPercentage(calculation.getRefundPercentage());
        ticket.setCancellationCharges(calculation.getCancellationCharges());
        ticket.setCancellationRequestedDate(now);
        ticket.setAdminRemarks(null);
        ticket.setCancellationDecisionDate(null);
        ticket.setRefundProcessedDate(null);
        ticket.setRefundDate(null);
        ticket.setRefundTransactionId(null);

        ticketRepository.save(ticket);

        return CancellationResponseDTO.builder()
                .ticketId(ticketId)
                .status("PENDING")
                .message("Cancellation request submitted successfully")
                .refundAmount(calculation.getRefundAmount())
                .cancellationCharges(calculation.getCancellationCharges())
                .cancellationTime(now)
                .refundStatus("PENDING")
                .success(true)
                .build();
    }

    @Override
    public CancellationResponseDTO processRefund(String ticketId, Long userId) {
        Ticket ticket = getTicketById(ticketId);
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized refund request.");
        }

        ticket.setRefundStatus("COMPLETED");
        ticket.setRefundDate(LocalDateTime.now());
        ticket.setRefundProcessedDate(LocalDateTime.now());
        ticketRepository.save(ticket);

        return CancellationResponseDTO.builder()
                .ticketId(ticketId)
                .status("REFUNDED")
                .message("Refund processed successfully")
                .refundAmount(ticket.getRefundAmount())
                .refundStatus("COMPLETED")
                .actualCompletionDate(LocalDateTime.now())
                .success(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RefundEstimateDTO calculateRefundEstimate(String ticketId, Long userId) {
        Ticket ticket = getTicketById(ticketId);
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized refund estimate request.");
        }

        RefundCalculationDTO calculation = buildRefundCalculation(
                ticket.getTotalFare(),
                getDepartureDateTime(ticket),
                LocalDateTime.now()
        );

        return RefundEstimateDTO.builder()
                .ticketId(ticketId)
                .originalFare(ticket.getTotalFare())
                .refundAmount(calculation.getRefundAmount())
                .cancellationCharges(calculation.getCancellationCharges())
                .refundPercentage(calculation.getRefundPercentage())
                .refundPolicy(calculation.getAppliedPolicy())
                .estimatedRefundDate(LocalDateTime.now().plusDays(3))
                .isEligible(calculation.getIsEligible())
                .eligibilityMessage(calculation.getMessage())
                .paymentMethod(ticket.getPaymentMethod())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RefundStatusDTO checkRefundStatus(String ticketId, Long userId) {
        Ticket ticket = getTicketById(ticketId);
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized refund status request.");
        }

        return buildRefundStatus(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundStatusDTO> getUserRefunds(Long userId) {
        List<Ticket> tickets = ticketRepository.findTicketsWithRefundByUserId(userId);
        List<RefundStatusDTO> results = new ArrayList<>();
        for (Ticket ticket : tickets) {
            results.add(buildRefundStatus(ticket));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRefundStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        List<Ticket> tickets = ticketRepository.findTicketsWithRefundByUserId(userId);
        long pending = tickets.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getRefundStatus())).count();
        long completed = tickets.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getRefundStatus())).count();

        stats.put("totalRefunds", tickets.size());
        stats.put("pendingRefunds", pending);
        stats.put("completedRefunds", completed);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUserRefundStats(Long userId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pendingRefunds", ticketRepository.countPendingRefundsByUserId(userId));
        stats.put("processingRefunds", ticketRepository.countProcessingRefundsByUserId(userId));
        stats.put("completedRefunds", ticketRepository.countCompletedRefundsByUserId(userId));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCancellable(String ticketId) {
        Ticket ticket = getTicketById(ticketId);
        return isTicketEligibleForCancellation(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundCalculationDTO calculateRefund(Double totalFare, LocalDate journeyDate, LocalDateTime bookingDate) {
        LocalDateTime cancellationTime = LocalDateTime.now();
        LocalDateTime departureTime = journeyDate.atTime(LocalTime.MIDNIGHT);
        return buildRefundCalculation(totalFare, departureTime, cancellationTime);
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationPolicyDTO getCancellationPolicy() {
        List<CancellationRule> rules = getOrCreateRules();
        List<RefundSlabDTO> slabs = rules.stream()
                .map(rule -> new RefundSlabDTO(
                        rule.getMinHoursBeforeDeparture(),
                        rule.getMaxHoursBeforeDeparture(),
                        rule.getRefundPercentage(),
                        rule.getDescription()))
                .toList();

        return CancellationPolicyDTO.builder()
                .refundSlabs(slabs)
                .minimumHoursForCancellation(0)
                .maximumRefundPercentage(slabs.stream()
                        .map(RefundSlabDTO::getRefundPercentage)
                        .max(Double::compareTo)
                        .orElse(0.0))
                .policyEffectiveDate(LocalDate.now().toString())
                .policyVersion("2.0")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkSeatAvailability(Integer trainNo, String classType,
                                         Integer numberOfSeats, LocalDate journeyDate) {
        return getAvailableSeats(trainNo, classType, journeyDate) >= numberOfSeats;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getBookedSeatsCount(Integer trainNo, LocalDate journeyDate, String classType) {
        return ticketRepository.countBookedSeatsByTrainAndDateAndClass(trainNo, journeyDate, normalizeClassType(classType));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByTicketStatus(status);
    }

    @Override
    public Ticket updateTicketStatus(String ticketId, TicketStatus status) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setTicketStatus(status);
        return ticketRepository.save(ticket);
    }

    @Override
    public void cancelTicket(String ticketId, Long userId, String reason) {
        processCancellation(ticketId, userId, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateRefundAmount(Double totalFare, LocalDate journeyDate) {
        RefundCalculationDTO calculation = calculateRefund(totalFare, journeyDate, LocalDateTime.now());
        return calculation.getRefundAmount();
    }

    private double resolveClassFare(Train train, String classType) {
        String normalizedClassType = normalizeClassType(classType);
        Double fare = switch (normalizedClassType) {
            case "AC" -> train.getAcFare();
            case "SLEEPER" -> train.getSleeperFare();
            case "GENERAL" -> train.getGeneralFare();
            default -> throw new RuntimeException("Unsupported class type: " + normalizedClassType);
        };

        if (fare == null || fare <= 0) {
            throw new RuntimeException("Fare not configured for class: " + normalizedClassType);
        }
        return fare;
    }

    private String normalizeClassType(String classType) {
        if (classType == null || classType.isBlank()) {
            throw new RuntimeException("Seat class selection is required.");
        }

        return switch (classType.trim().toUpperCase()) {
            case "AC", "AC_SEATS", "AC-SEATS" -> "AC";
            case "SLEEPER", "SLEEPER_SEATS", "SLEEPER-SEATS" -> "SLEEPER";
            case "GENERAL", "GENERAL_SEATS", "GENERAL-SEATS" -> "GENERAL";
            default -> throw new RuntimeException("Unsupported class type: " + classType);
        };
    }

    private RefundStatusDTO buildRefundStatus(Ticket ticket) {
        String refundStatus = ticket.getRefundStatus() != null ? ticket.getRefundStatus() : "NA";
        return RefundStatusDTO.builder()
                .ticketId(ticket.getTicketId())
                .ticketNumber(ticket.getTicketId())
                .trainName(ticket.getTrain().getTrainName())
                .passengerName(ticket.getPassengerName())
                .cancellationDate(ticket.getCancellationDate())
                .originalFare(ticket.getTotalFare())
                .cancellationCharges(ticket.getCancellationCharges())
                .refundAmount(ticket.getRefundAmount())
                .refundStatus(refundStatus)
                .refundStatusMessage(resolveRefundStatusMessage(refundStatus))
                .refundTransactionId(ticket.getRefundTransactionId())
                .paymentMethod(ticket.getPaymentMethod())
                .refundMethod(ticket.getPaymentMethod())
                .estimatedCompletionDate(ticket.getRefundProcessedDate() == null
                        && ticket.getCancellationDecisionDate() != null
                        ? ticket.getCancellationDecisionDate().plusDays(3)
                        : null)
                .actualCompletionDate(ticket.getRefundProcessedDate())
                .timeline(buildRefundTimeline(ticket))
                .build();
    }

    private RefundCalculationDTO buildRefundCalculation(Double totalFare,
                                                        LocalDateTime departureTime,
                                                        LocalDateTime cancellationTime) {
        if (totalFare == null || totalFare <= 0) {
            return RefundCalculationDTO.builder()
                    .originalFare(totalFare)
                    .refundAmount(0.0)
                    .cancellationCharges(0.0)
                    .refundPercentage(0.0)
                    .appliedPolicy("Invalid fare")
                    .isEligible(false)
                    .message("Invalid fare for refund calculation.")
                    .build();
        }

        long hoursBeforeDeparture = ChronoUnit.HOURS.between(cancellationTime, departureTime);
        if (hoursBeforeDeparture <= 0) {
            return RefundCalculationDTO.builder()
                    .originalFare(totalFare)
                    .refundAmount(0.0)
                    .cancellationCharges(totalFare)
                    .refundPercentage(0.0)
                    .appliedPolicy("No refund after departure")
                    .hoursBeforeDeparture((int) hoursBeforeDeparture)
                    .isEligible(false)
                    .message("Cancellation not eligible after departure.")
                    .build();
        }

        CancellationRule applicableRule = findApplicableRule(departureTime, cancellationTime).orElse(null);
        double refundPercentage = applicableRule != null ? applicableRule.getRefundPercentage() : 0.0;
        String policy = applicableRule != null ? applicableRule.getDescription() : "No refund rule matched";

        double refundAmount = totalFare * (refundPercentage / 100);
        double cancellationCharges = totalFare - refundAmount;

        return RefundCalculationDTO.builder()
                .originalFare(totalFare)
                .refundAmount(refundAmount)
                .cancellationCharges(cancellationCharges)
                .refundPercentage(refundPercentage)
                .appliedPolicy(policy)
                .hoursBeforeDeparture((int) hoursBeforeDeparture)
                .isEligible(true)
                .message(refundPercentage > 0
                        ? "Refund eligible as per policy."
                        : "Cancellation allowed but no refund is applicable.")
                .build();
    }

    private LocalDateTime getDepartureDateTime(Ticket ticket) {
        LocalTime departureTime = ticket.getTrain() != null && ticket.getTrain().getDepartureTime() != null
                ? ticket.getTrain().getDepartureTime()
                : LocalTime.MIDNIGHT;
        return ticket.getJourneyDate().atTime(departureTime);
    }

    private boolean isTicketEligibleForCancellation(Ticket ticket) {
        return ticket.getTicketStatus() == TicketStatus.CONFIRMED
                && "CONFIRMED".equalsIgnoreCase(ticket.getBookingStatus())
                && getDepartureDateTime(ticket).isAfter(LocalDateTime.now());
    }

    private List<CancellationRule> getOrCreateRules() {
        List<CancellationRule> rules = cancellationRuleRepository.findActiveRules();
        if (!rules.isEmpty()) {
            return rules;
        }

        List<CancellationRule> defaults = List.of(
                CancellationRule.builder().minHoursBeforeDeparture(24).maxHoursBeforeDeparture(null)
                        .refundPercentage(100.0).description("24+ hours before departure").isActive(true).build(),
                CancellationRule.builder().minHoursBeforeDeparture(12).maxHoursBeforeDeparture(24)
                        .refundPercentage(50.0).description("12-24 hours before departure").isActive(true).build(),
                CancellationRule.builder().minHoursBeforeDeparture(0).maxHoursBeforeDeparture(12)
                        .refundPercentage(0.0).description("Less than 12 hours before departure").isActive(true).build()
        );
        return cancellationRuleRepository.saveAll(defaults);
    }

    private java.util.Optional<CancellationRule> findApplicableRule(LocalDateTime departureTime, LocalDateTime cancellationTime) {
        long hoursBeforeDeparture = Math.max(0, ChronoUnit.HOURS.between(cancellationTime, departureTime));
        return getOrCreateRules().stream()
                .filter(rule -> hoursBeforeDeparture >= rule.getMinHoursBeforeDeparture())
                .filter(rule -> rule.getMaxHoursBeforeDeparture() == null
                        || hoursBeforeDeparture < rule.getMaxHoursBeforeDeparture())
                .findFirst();
    }

    private String resolveRefundStatusMessage(String refundStatus) {
        return switch (refundStatus.toUpperCase()) {
            case "PENDING" -> "Ticket refund status is pending.";
            case "PROCESSING" -> "Ticket refund status is under processing.";
            case "COMPLETED" -> "Ticket refund status is completed.";
            case "DECLINED" -> "Ticket refund request was declined.";
            default -> "Ticket refund status is unavailable.";
        };
    }

    private List<RefundTimelineDTO> buildRefundTimeline(Ticket ticket) {
        List<RefundTimelineDTO> timeline = new ArrayList<>();
        timeline.add(RefundTimelineDTO.builder()
                .stage("Cancellation Requested")
                .message(ticket.getCancellationReason() != null
                        ? "Reason: " + ticket.getCancellationReason()
                        : "Your cancellation request was submitted.")
                .timestamp(ticket.getCancellationRequestedDate())
                .completed(ticket.getCancellationRequestedDate() != null)
                .build());
        timeline.add(RefundTimelineDTO.builder()
                .stage("Admin Review")
                .message(ticket.getCancellationDecisionDate() != null
                        ? (ticket.getAdminRemarks() != null ? ticket.getAdminRemarks() : "Reviewed by admin")
                        : "Awaiting admin review")
                .timestamp(ticket.getCancellationDecisionDate())
                .completed(ticket.getCancellationDecisionDate() != null)
                .build());
        timeline.add(RefundTimelineDTO.builder()
                .stage("Refund Processing")
                .message("PROCESSING".equalsIgnoreCase(ticket.getRefundStatus()) || "COMPLETED".equalsIgnoreCase(ticket.getRefundStatus())
                        ? "Refund is being sent to the original payment source"
                        : "Refund processing has not started")
                .timestamp("PROCESSING".equalsIgnoreCase(ticket.getRefundStatus()) || "COMPLETED".equalsIgnoreCase(ticket.getRefundStatus())
                        ? ticket.getCancellationDecisionDate()
                        : null)
                .completed("PROCESSING".equalsIgnoreCase(ticket.getRefundStatus()) || "COMPLETED".equalsIgnoreCase(ticket.getRefundStatus()))
                .build());
        timeline.add(RefundTimelineDTO.builder()
                .stage("Refund Completed")
                .message(ticket.getRefundProcessedDate() != null
                        ? "Refund transaction completed"
                        : "Awaiting refund completion")
                .timestamp(ticket.getRefundProcessedDate())
                .completed(ticket.getRefundProcessedDate() != null)
                .build());
        return timeline;
    }
}
