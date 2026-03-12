package com.railease.service.impl;

import com.railease.constants.TicketStatus;
import com.railease.dto.*;
import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
import com.railease.exception.TrainNotFoundException;
import com.railease.exception.UserNotFoundException;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private final TicketRepository ticketRepository;
    private final TrainRepository trainRepository;
    private final UserRepository userRepository;
    private final TicketIdGenerator ticketIdGenerator;

    private static final double GST_RATE = 0.05;
    private static final int MIN_HOURS_FOR_CANCELLATION = 2;

    // ==================== BASIC BOOKING OPERATIONS ====================

    @Override
    public Ticket createTicket(Long userId, Integer trainNo, String passengerName,
                               Integer passengerAge, String passengerGender,
                               String classType, Integer numberOfSeats, LocalDate journeyDate) {

        log.info("Creating ticket for user: {}, train: {}", userId, trainNo);

        // Validate inputs
        if (numberOfSeats < 1 || numberOfSeats > 6) {
            throw new RuntimeException("Number of seats must be between 1 and 6");
        }

        if (passengerAge < 1 || passengerAge > 120) {
            throw new RuntimeException("Invalid age. Age must be between 1 and 120");
        }

        if (!List.of("MALE", "FEMALE", "OTHER").contains(passengerGender)) {
            throw new RuntimeException("Invalid gender selection");
        }

        if (journeyDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Journey date cannot be in the past");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        // Use Integer directly for repository lookup
        Train train = trainRepository.findById(trainNo)
                .orElseThrow(() -> new TrainNotFoundException("Train not found: " + trainNo));

        if (!train.getIsActive()) {
            throw new RuntimeException("Train is not active");
        }

        // Check seat availability
        if (!checkSeatAvailability(trainNo, classType, numberOfSeats, journeyDate)) {
            throw new RuntimeException("Not enough seats available. Only " +
                    getAvailableSeats(trainNo, classType, journeyDate) + " seats left.");
        }

        // Calculate fare with GST
        double baseFare = calculateBaseFare(train, classType, numberOfSeats);
        double gst = baseFare * GST_RATE;
        double totalFare = baseFare + gst;

        // Generate ticket ID
        String ticketId = ticketIdGenerator.generateTicketId();

        Ticket ticket = Ticket.builder()
                .ticketId(ticketId)
                .user(user)
                .train(train)
                .bookingDate(LocalDateTime.now())
                .journeyDate(journeyDate)
                .sourceStation(train.getSource())
                .destinationStation(train.getDestination())
                .passengerName(passengerName)
                .passengerAge(passengerAge)
                .passengerGender(passengerGender)
                .classType(classType.toUpperCase())
                .numberOfSeats(numberOfSeats)
                .totalFare(totalFare)
                .ticketStatus(TicketStatus.PENDING)
                .bookingStatus("PENDING")
                .paymentStatus("PENDING")
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created successfully with ID: {}", ticketId);

        return savedTicket;
    }

    @Override
    public Ticket confirmBooking(String ticketId, String paymentId, String paymentMethod) {
        log.info("Confirming booking for ticket: {}", ticketId);

        Ticket ticket = getTicketById(ticketId);

        if (ticket.getTicketStatus() != TicketStatus.PENDING) {
            throw new RuntimeException("Ticket is not in PENDING state");
        }

        // Update seat availability
        Train train = ticket.getTrain();
        int newAvailableSeats = train.getAvailableSeats() - ticket.getNumberOfSeats();
        if (newAvailableSeats < 0) {
            throw new RuntimeException("Seat availability changed. Please try again.");
        }
        train.setAvailableSeats(newAvailableSeats);
        trainRepository.save(train);

        ticket.setPaymentId(paymentId);
        ticket.setPaymentMethod(paymentMethod);
        ticket.setPaymentStatus("PAID");
        ticket.setBookingStatus("CONFIRMED");
        ticket.setTicketStatus(TicketStatus.CONFIRMED);

        Ticket confirmedTicket = ticketRepository.save(ticket);
        log.info("Booking confirmed successfully: {}", ticketId);

        return confirmedTicket;
    }

    @Override
    public Ticket getTicketById(String ticketId) {
        return ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + ticketId));
    }

    @Override
    public List<Ticket> getUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return ticketRepository.findByUserOrderByBookingDateDesc(user);
    }

    @Override
    public List<Ticket> getUserRecentBookings(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return ticketRepository.findRecentByUser(user, PageRequest.of(0, limit));
    }

    @Override
    public List<Ticket> getUserActiveBookings(Long userId) {
        log.info("Fetching active bookings for user: {}", userId);
        return ticketRepository.findActiveTicketsByUserId(userId);
    }

    @Override
    public List<Ticket> getUserPastBookings(Long userId) {
        log.info("Fetching past bookings for user: {}", userId);
        return ticketRepository.findPastTicketsByUserId(userId);
    }

    @Override
    public List<Ticket> getUserCancelledBookings(Long userId) {
        return ticketRepository.findCancelledTicketsByUserId(userId);
    }

    @Override
    public List<Ticket> getUserRefundedBookings(Long userId) {
        return ticketRepository.findByUserIdAndRefundStatus(userId, "COMPLETED");
    }

    @Override
    public Long getUserBookingCount(Long userId) {
        return ticketRepository.countByUserId(userId);
    }

    @Override
    public Long getUserUpcomingJourneysCount(Long userId) {
        return ticketRepository.countUpcomingByUserId(userId);
    }

    // ==================== CANCELLATION & REFUND METHODS ====================

    @Override
    public CancellationResponseDTO initiateCancellation(String ticketId, Long userId, CancellationRequestDTO request) {
        log.info("Initiating cancellation for ticket: {} by user: {}", ticketId, userId);

        Ticket ticket = getTicketById(ticketId);

        // Verify ownership
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this ticket");
        }

        // Check if ticket can be cancelled
        if (!isCancellable(ticketId)) {
            throw new RuntimeException("Ticket cannot be cancelled at this time");
        }

        // Calculate refund estimate
        RefundCalculationDTO refundCalc = calculateRefund(
                ticket.getTotalFare(),
                ticket.getJourneyDate(),
                ticket.getBookingDate()
        );

        if (!refundCalc.getIsEligible()) {
            throw new RuntimeException(refundCalc.getMessage());
        }

        // Set cancellation request
        ticket.setCancellationRequestedDate(LocalDateTime.now());
        ticket.setCancellationReason(request.getReason());
        ticket.setRefundStatus("PENDING");
        ticketRepository.save(ticket);

        log.info("Cancellation initiated successfully for ticket: {}", ticketId);

        return CancellationResponseDTO.builder()
                .ticketId(ticketId)
                .status("CANCELLATION_REQUESTED")
                .message("Cancellation request submitted successfully")
                .refundAmount(refundCalc.getRefundAmount())
                .cancellationCharges(refundCalc.getCancellationCharges())
                .cancellationTime(LocalDateTime.now())
                .refundStatus("PENDING")
                .estimatedRefundDate(getEstimatedRefundDate())
                .success(true)
                .build();
    }

    @Override
    public CancellationResponseDTO processCancellation(String ticketId, Long userId, String reason) {
        log.info("Processing cancellation for ticket: {}", ticketId);

        Ticket ticket = getTicketById(ticketId);

        // Verify ownership
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this ticket");
        }

        // Check if already cancelled
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            throw new RuntimeException("Ticket already cancelled");
        }

        // Check if ticket can be cancelled
        if (!isCancellable(ticketId)) {
            throw new RuntimeException("Ticket cannot be cancelled at this time");
        }

        // Calculate refund
        RefundCalculationDTO refundCalc = calculateRefund(
                ticket.getTotalFare(),
                ticket.getJourneyDate(),
                ticket.getBookingDate()
        );

        // Update seat availability (add seats back)
        Train train = ticket.getTrain();
        train.setAvailableSeats(train.getAvailableSeats() + ticket.getNumberOfSeats());
        trainRepository.save(train);

        // Update ticket
        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setBookingStatus("CANCELLED");
        ticket.setPaymentStatus("REFUND_PENDING");
        ticket.setCancellationDate(LocalDateTime.now());
        ticket.setCancellationReason(reason);
        ticket.setRefundAmount(refundCalc.getRefundAmount());
        ticket.setCancellationCharges(refundCalc.getCancellationCharges());
        ticket.setRefundStatus("PROCESSING");

        ticketRepository.save(ticket);

        log.info("Cancellation processed successfully for ticket: {}", ticketId);

        return CancellationResponseDTO.builder()
                .ticketId(ticketId)
                .status("CANCELLED")
                .message("Ticket cancelled successfully")
                .refundAmount(refundCalc.getRefundAmount())
                .cancellationCharges(refundCalc.getCancellationCharges())
                .cancellationTime(LocalDateTime.now())
                .refundStatus("PROCESSING")
                .estimatedRefundDate("5-7 business days")
                .success(true)
                .build();
    }

    @Override
    public CancellationResponseDTO processRefund(String ticketId, Long userId) {
        log.info("Processing refund for ticket: {}", ticketId);

        Ticket ticket = getTicketById(ticketId);

        // Verify ownership
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to process refund");
        }

        if (ticket.getTicketStatus() != TicketStatus.CANCELLED) {
            throw new RuntimeException("Ticket is not cancelled");
        }

        if ("COMPLETED".equals(ticket.getRefundStatus())) {
            throw new RuntimeException("Refund already processed");
        }

        // Generate refund transaction ID
        String refundTransactionId = "REF" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        ticket.setRefundStatus("COMPLETED");
        ticket.setRefundDate(LocalDateTime.now());
        ticket.setRefundTransactionId(refundTransactionId);
        ticket.setPaymentStatus("REFUNDED");

        ticketRepository.save(ticket);

        return CancellationResponseDTO.builder()
                .ticketId(ticketId)
                .status("REFUNDED")
                .message("Refund processed successfully")
                .refundAmount(ticket.getRefundAmount())
                .cancellationTime(ticket.getCancellationDate())
                .refundStatus("COMPLETED")
                .transactionId(refundTransactionId)
                .success(true)
                .build();
    }

    @Override
    public RefundEstimateDTO calculateRefundEstimate(String ticketId, Long userId) {
        log.info("Calculating refund estimate for ticket: {}", ticketId);

        Ticket ticket = getTicketById(ticketId);

        // Verify ownership
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to view this ticket");
        }

        RefundCalculationDTO refundCalc = calculateRefund(
                ticket.getTotalFare(),
                ticket.getJourneyDate(),
                ticket.getBookingDate()
        );

        RefundEstimateDTO estimate = new RefundEstimateDTO();
        estimate.setTicketId(ticketId);
        estimate.setOriginalFare(ticket.getTotalFare());
        estimate.setRefundAmount(refundCalc.getRefundAmount());
        estimate.setCancellationCharges(refundCalc.getCancellationCharges());
        estimate.setRefundPercentage(refundCalc.getRefundPercentage());
        estimate.setRefundPolicy(refundCalc.getAppliedPolicy());
        estimate.setEstimatedRefundDate(LocalDateTime.now().plusDays(5));
        estimate.setIsEligible(refundCalc.getIsEligible());
        estimate.setEligibilityMessage(refundCalc.getMessage());

        // Add payment method details if available
        if (ticket.getPaymentMethod() != null) {
            estimate.setPaymentMethod(ticket.getPaymentMethod());
            if ("CARD".equals(ticket.getPaymentMethod()) && ticket.getPaymentId() != null) {
                String paymentId = ticket.getPaymentId();
                if (paymentId.length() > 4) {
                    estimate.setLastFourDigits(paymentId.substring(paymentId.length() - 4));
                }
            }
        }

        return estimate;
    }

    @Override
    public RefundStatusDTO checkRefundStatus(String ticketId, Long userId) {
        log.info("Checking refund status for ticket: {}", ticketId);

        Ticket ticket = getTicketById(ticketId);

        // Verify ownership
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to view this ticket");
        }

        RefundStatusDTO status = new RefundStatusDTO();
        status.setTicketId(ticketId);
        status.setTicketNumber(ticket.getTicketId());
        status.setTrainName(ticket.getTrain().getTrainName());
        status.setPassengerName(ticket.getPassengerName());
        status.setCancellationDate(ticket.getCancellationDate());
        status.setOriginalFare(ticket.getTotalFare());
        status.setCancellationCharges(ticket.getCancellationCharges());
        status.setRefundAmount(ticket.getRefundAmount());
        status.setRefundStatus(ticket.getRefundStatus());
        status.setRefundTransactionId(ticket.getRefundTransactionId());
        status.setPaymentMethod(ticket.getPaymentMethod());

        // Set status message based on refund status
        switch (ticket.getRefundStatus() != null ? ticket.getRefundStatus() : "PENDING") {
            case "PENDING":
                status.setRefundStatusMessage("Refund request received and is pending approval");
                status.setEstimatedCompletionDate(LocalDateTime.now().plusDays(1));
                break;
            case "PROCESSING":
                status.setRefundStatusMessage("Refund is being processed by your bank");
                status.setEstimatedCompletionDate(LocalDateTime.now().plusDays(3));
                break;
            case "COMPLETED":
                status.setRefundStatusMessage("Refund has been successfully processed");
                status.setActualCompletionDate(ticket.getRefundDate());
                break;
            case "REJECTED":
                status.setRefundStatusMessage("Refund request was rejected");
                break;
            default:
                status.setRefundStatusMessage("Refund status unknown");
        }

        // Build timeline
        List<RefundTimelineDTO> timeline = new ArrayList<>();

        timeline.add(createTimelineStage("Cancellation Requested",
                ticket.getCancellationRequestedDate() != null, ticket.getCancellationRequestedDate(),
                "Your cancellation request has been submitted"));

        timeline.add(createTimelineStage("Request Approved",
                ticket.getCancellationDate() != null, ticket.getCancellationDate(),
                "Your cancellation has been approved"));

        timeline.add(createTimelineStage("Refund Processing",
                "PROCESSING".equals(ticket.getRefundStatus()) || "COMPLETED".equals(ticket.getRefundStatus()),
                ticket.getRefundDate() != null ? ticket.getRefundDate().minusDays(1) : null,
                "Refund is being processed by your bank"));

        timeline.add(createTimelineStage("Refund Completed",
                "COMPLETED".equals(ticket.getRefundStatus()),
                ticket.getRefundDate(),
                "Refund has been credited to your account"));

        status.setTimeline(timeline);

        return status;
    }

    @Override
    public List<RefundStatusDTO> getUserRefunds(Long userId) {
        log.info("Fetching refunds for user: {}", userId);

        List<Ticket> refundTickets = ticketRepository.findTicketsWithRefundByUserId(userId);

        return refundTickets.stream()
                .map(ticket -> {
                    try {
                        return checkRefundStatus(ticket.getTicketId(), userId);
                    } catch (Exception e) {
                        log.error("Error processing refund status for ticket: {}", ticket.getTicketId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRefundStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        List<Ticket> refundedTickets = ticketRepository.findByUserIdAndRefundStatus(userId, "COMPLETED");
        List<Ticket> pendingRefunds = ticketRepository.findByUserIdAndRefundStatus(userId, "PENDING");
        List<Ticket> processingRefunds = ticketRepository.findByUserIdAndRefundStatus(userId, "PROCESSING");

        Double totalRefunded = refundedTickets.stream()
                .mapToDouble(Ticket::getRefundAmount)
                .sum();

        stats.put("totalRefunds", refundedTickets.size());
        stats.put("pendingRefunds", pendingRefunds.size());
        stats.put("processingRefunds", processingRefunds.size());
        stats.put("totalRefundedAmount", totalRefunded);
        stats.put("averageRefundTime", "3-5 business days");

        return stats;
    }

    @Override
    public Map<String, Long> getUserRefundStats(Long userId) {
        Map<String, Long> stats = new HashMap<>();

        Long pendingRefunds = ticketRepository.countPendingRefundsByUserId(userId);
        Long processingRefunds = ticketRepository.countProcessingRefundsByUserId(userId);
        Long completedRefunds = ticketRepository.countCompletedRefundsByUserId(userId);

        stats.put("pendingRefunds", pendingRefunds != null ? pendingRefunds : 0L);
        stats.put("processingRefunds", processingRefunds != null ? processingRefunds : 0L);
        stats.put("completedRefunds", completedRefunds != null ? completedRefunds : 0L);

        return stats;
    }

    @Override
    public boolean isCancellable(String ticketId) {
        Ticket ticket = getTicketById(ticketId);

        // Can't cancel if already cancelled
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            return false;
        }

        // Can't cancel if journey date is in the past
        if (ticket.getJourneyDate().isBefore(LocalDate.now())) {
            return false;
        }

        // Can't cancel if within minimum hours of departure
        LocalDateTime departureDateTime = ticket.getJourneyDate()
                .atTime(ticket.getTrain().getDepartureTime());
        if (departureDateTime.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_FOR_CANCELLATION))) {
            return false;
        }

        return true;
    }

    @Override
    public RefundCalculationDTO calculateRefund(Double totalFare, LocalDate journeyDate, LocalDateTime bookingDate) {
        long hoursUntilJourney = ChronoUnit.HOURS.between(LocalDateTime.now(),
                journeyDate.atTime(23, 59));

        double refundPercentage;
        String appliedPolicy;

        // Refund policy based on time before departure
        if (hoursUntilJourney > 48 * 24) { // More than 48 days
            refundPercentage = 0.90;
            appliedPolicy = "90% refund for cancellations more than 48 days before journey";
        } else if (hoursUntilJourney > 30 * 24) { // 30-48 days
            refundPercentage = 0.75;
            appliedPolicy = "75% refund for cancellations 30-48 days before journey";
        } else if (hoursUntilJourney > 15 * 24) { // 15-30 days
            refundPercentage = 0.60;
            appliedPolicy = "60% refund for cancellations 15-30 days before journey";
        } else if (hoursUntilJourney > 7 * 24) { // 7-15 days
            refundPercentage = 0.50;
            appliedPolicy = "50% refund for cancellations 7-15 days before journey";
        } else if (hoursUntilJourney > 3 * 24) { // 3-7 days
            refundPercentage = 0.40;
            appliedPolicy = "40% refund for cancellations 3-7 days before journey";
        } else if (hoursUntilJourney > 24) { // 1-3 days
            refundPercentage = 0.30;
            appliedPolicy = "30% refund for cancellations 1-3 days before journey";
        } else if (hoursUntilJourney > 2) { // 2-24 hours
            refundPercentage = 0.20;
            appliedPolicy = "20% refund for cancellations 2-24 hours before journey";
        } else {
            refundPercentage = 0.0;
            appliedPolicy = "No refund for cancellations within 2 hours of departure";
        }

        double refundAmount = totalFare * refundPercentage;
        double cancellationCharges = totalFare - refundAmount;

        boolean isEligible = refundPercentage > 0;
        String message = isEligible ?
                String.format("Refund eligible: %.0f%% of fare (₹%.2f)", refundPercentage * 100, refundAmount) :
                "No refund applicable for this cancellation";

        return RefundCalculationDTO.builder()
                .originalFare(totalFare)
                .refundAmount(refundAmount)
                .cancellationCharges(cancellationCharges)
                .refundPercentage(refundPercentage * 100)
                .appliedPolicy(appliedPolicy)
                .hoursBeforeDeparture((int) hoursUntilJourney)
                .isEligible(isEligible)
                .message(message)
                .build();
    }

    @Override
    public CancellationPolicyDTO getCancellationPolicy() {
        CancellationPolicyDTO policy = new CancellationPolicyDTO();

        List<RefundSlabDTO> slabs = new ArrayList<>();
        slabs.add(createRefundSlab(48 * 24, null, 90, "More than 48 days before journey"));
        slabs.add(createRefundSlab(30 * 24, 48 * 24, 75, "30-48 days before journey"));
        slabs.add(createRefundSlab(15 * 24, 30 * 24, 60, "15-30 days before journey"));
        slabs.add(createRefundSlab(7 * 24, 15 * 24, 50, "7-15 days before journey"));
        slabs.add(createRefundSlab(3 * 24, 7 * 24, 40, "3-7 days before journey"));
        slabs.add(createRefundSlab(24, 3 * 24, 30, "1-3 days before journey"));
        slabs.add(createRefundSlab(2, 24, 20, "2-24 hours before journey"));

        policy.setRefundSlabs(slabs);

        Map<String, String> terms = new HashMap<>();
        terms.put("minimumHours", "Cancellations must be made at least 2 hours before departure");
        terms.put("processingTime", "Refunds are processed within 5-7 business days");
        terms.put("paymentMethod", "Refunds are credited to original payment method");
        terms.put("partialCancellation", "Partial cancellation of seats is not allowed");

        policy.setTerms(terms);
        policy.setMinimumHoursForCancellation(2);
        policy.setMaximumRefundPercentage(90.0);
        policy.setPolicyEffectiveDate("01-Jan-2026");
        policy.setPolicyVersion("2.0");

        return policy;
    }

    @Override
    public boolean checkSeatAvailability(Integer trainNo, String classType,
                                         Integer numberOfSeats, LocalDate journeyDate) {
        Train train = trainRepository.findById(trainNo)
                .orElseThrow(() -> new TrainNotFoundException("Train not found: " + trainNo));

        int availableSeats = getAvailableSeats(trainNo, classType, journeyDate);
        return availableSeats >= numberOfSeats;
    }

    @Override
    public Long getBookedSeatsCount(Integer trainNo, LocalDate journeyDate, String classType) {
        return ticketRepository.countBookedSeatsByTrainAndDateAndClass(trainNo, journeyDate, classType.toUpperCase());
    }

    @Override
    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByTicketStatus(status);
    }

    @Override
    public Ticket updateTicketStatus(String ticketId, TicketStatus status) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setTicketStatus(status);

        if (status == TicketStatus.CONFIRMED) {
            ticket.setBookingStatus("CONFIRMED");
        } else if (status == TicketStatus.CANCELLED) {
            ticket.setBookingStatus("CANCELLED");
            ticket.setPaymentStatus("REFUNDED");
            ticket.setCancellationDate(LocalDateTime.now());
        }

        return ticketRepository.save(ticket);
    }

    @Override
    public void cancelTicket(String ticketId, Long userId, String reason) {
        log.info("Cancelling ticket: {} for user: {}", ticketId, userId);

        Ticket ticket = getTicketById(ticketId);

        // Verify ownership
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this ticket");
        }

        // Check if ticket can be cancelled
        if (!isCancellable(ticketId)) {
            throw new RuntimeException("Ticket cannot be cancelled at this time");
        }

        // Calculate refund amount
        Double refundAmount = calculateRefundAmount(ticket.getTotalFare(), ticket.getJourneyDate());

        // Update seat availability (add seats back)
        Train train = ticket.getTrain();
        train.setAvailableSeats(train.getAvailableSeats() + ticket.getNumberOfSeats());
        trainRepository.save(train);

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setBookingStatus("CANCELLED");
        ticket.setPaymentStatus("REFUNDED");
        ticket.setCancellationDate(LocalDateTime.now());
        ticket.setCancellationReason(reason);
        ticket.setRefundAmount(refundAmount);

        ticketRepository.save(ticket);
        log.info("Ticket cancelled successfully: {}, refund amount: ₹{}", ticketId, refundAmount);
    }

    @Override
    public Double calculateRefundAmount(Double totalFare, LocalDate journeyDate) {
        long daysUntilJourney = ChronoUnit.DAYS.between(LocalDate.now(), journeyDate);

        // Refund policy based on days before journey
        if (daysUntilJourney > 7) {
            return totalFare * 0.9; // 90% refund if more than 7 days
        } else if (daysUntilJourney > 3) {
            return totalFare * 0.8; // 80% refund if 4-7 days
        } else if (daysUntilJourney > 1) {
            return totalFare * 0.6; // 60% refund if 2-3 days
        } else if (daysUntilJourney > 0) {
            return totalFare * 0.5; // 50% refund if 1 day
        } else {
            return 0.0; // No refund after departure
        }
    }

    // ==================== HELPER METHODS ====================

    private double calculateBaseFare(Train train, String classType, int numberOfSeats) {
        // Use class-specific fare based on classType
        double classFare;
        if ("AC".equalsIgnoreCase(classType)) {
            classFare = (train.getAcFare() != null && train.getAcFare() > 0) ? train.getAcFare() : train.getTicketPrice();
        } else if ("SLEEPER".equalsIgnoreCase(classType)) {
            classFare = (train.getSleeperFare() != null && train.getSleeperFare() > 0) ? train.getSleeperFare() : train.getTicketPrice();
        } else if ("GENERAL".equalsIgnoreCase(classType)) {
            classFare = (train.getGeneralFare() != null && train.getGeneralFare() > 0) ? train.getGeneralFare() : train.getTicketPrice();
        } else {
            // Default to ticketPrice if classType is not recognized
            classFare = train.getTicketPrice();
        }
        return classFare * numberOfSeats;
    }


    private RefundSlabDTO createRefundSlab(Integer fromHours, Integer toHours,
                                           Integer refundPercentage, String description) {
        RefundSlabDTO slab = new RefundSlabDTO();
        slab.setFromHours(fromHours);
        slab.setToHours(toHours);
        slab.setRefundPercentage(Double.valueOf(refundPercentage));
        slab.setDescription(description);
        return slab;
    }

    private String getEstimatedRefundDate() {
        return LocalDateTime.now().plusDays(5).toLocalDate().toString();
    }

    private RefundTimelineDTO createTimelineStage(String stage, boolean completed,
                                                  LocalDateTime timestamp, String message) {
        RefundTimelineDTO timeline = new RefundTimelineDTO();
        timeline.setStage(stage);
        timeline.setStatus(completed ? "COMPLETED" : "PENDING");
        timeline.setTimestamp(timestamp);
        timeline.setMessage(message);
        timeline.setIsCompleted(completed);
        return timeline;
    }

    @Override
    public Double getTotalSpentByUser(Long userId) {
        log.info("Calculating total spent for user: {}", userId);
        List<Ticket> userTickets = getUserBookings(userId);
        return userTickets.stream()
                .filter(ticket -> ticket.getPaymentStatus() != null &&
                        "PAID".equals(ticket.getPaymentStatus()))
                .mapToDouble(Ticket::getTotalFare)
                .sum();
    }

    @Override
    public int getAvailableSeats(Integer trainNo, String classType, LocalDate journeyDate) {
        log.info("Getting available seats for train: {}, class: {}, date: {}",
                trainNo, classType, journeyDate);

        Train train = trainRepository.findById(trainNo)
                .orElseThrow(() -> new TrainNotFoundException("Train not found: " + trainNo));

        Long bookedSeats = getBookedSeatsCount(trainNo, journeyDate, classType);
        return Math.max(0, train.getAvailableSeats() - (bookedSeats != null ? bookedSeats.intValue() : 0));
    }
}


