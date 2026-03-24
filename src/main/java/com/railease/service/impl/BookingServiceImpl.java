package com.railease.service.impl;

import com.railease.constants.TicketStatus;
import com.railease.dto.*;
import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
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

        RefundCalculationDTO calculation = calculateRefund(
                ticket.getTotalFare(),
                ticket.getJourneyDate(),
                ticket.getBookingDate()
        );

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setBookingStatus("CANCELLED");
        ticket.setCancellationDate(LocalDateTime.now());
        ticket.setCancellationReason(reason);
        ticket.setRefundStatus("PENDING");
        ticket.setRefundAmount(calculation.getRefundAmount());
        ticket.setRefundPercentage(calculation.getRefundPercentage());
        ticket.setCancellationCharges(calculation.getCancellationCharges());
        ticket.setCancellationRequestedDate(LocalDateTime.now());

        ticketRepository.save(ticket);

        return CancellationResponseDTO.builder()
                .ticketId(ticketId)
                .status("CANCELLED")
                .message("Cancellation processed successfully")
                .refundAmount(calculation.getRefundAmount())
                .cancellationCharges(calculation.getCancellationCharges())
                .cancellationTime(LocalDateTime.now())
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

        RefundCalculationDTO calculation = calculateRefund(
                ticket.getTotalFare(),
                ticket.getJourneyDate(),
                ticket.getBookingDate()
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
        LocalDateTime departureTime = getDepartureDateTime(ticket);
        return departureTime.isAfter(LocalDateTime.now())
                && ticket.getTicketStatus() != TicketStatus.CANCELLED;
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
        List<RefundSlabDTO> slabs = List.of(
                new RefundSlabDTO(48, null, 90.0, "48+ hours before departure"),
                new RefundSlabDTO(24, 48, 70.0, "24-48 hours before departure"),
                new RefundSlabDTO(12, 24, 50.0, "12-24 hours before departure"),
                new RefundSlabDTO(0, 12, 20.0, "Up to 12 hours before departure")
        );

        return CancellationPolicyDTO.builder()
                .refundSlabs(slabs)
                .minimumHoursForCancellation(0)
                .maximumRefundPercentage(90.0)
                .policyEffectiveDate(LocalDate.now().toString())
                .policyVersion("1.0")
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
        return RefundStatusDTO.builder()
                .ticketId(ticket.getTicketId())
                .trainName(ticket.getTrain().getTrainName())
                .passengerName(ticket.getPassengerName())
                .cancellationDate(ticket.getCancellationDate())
                .originalFare(ticket.getTotalFare())
                .cancellationCharges(ticket.getCancellationCharges())
                .refundAmount(ticket.getRefundAmount())
                .refundStatus(ticket.getRefundStatus())
                .refundTransactionId(ticket.getRefundTransactionId())
                .paymentMethod(ticket.getPaymentMethod())
                .actualCompletionDate(ticket.getRefundProcessedDate())
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

        double refundPercentage;
        String policy;
        if (hoursBeforeDeparture > 48) {
            refundPercentage = 90.0;
            policy = "48+ hours before departure";
        } else if (hoursBeforeDeparture > 24) {
            refundPercentage = 70.0;
            policy = "24-48 hours before departure";
        } else if (hoursBeforeDeparture > 12) {
            refundPercentage = 50.0;
            policy = "12-24 hours before departure";
        } else {
            refundPercentage = 20.0;
            policy = "Up to 12 hours before departure";
        }

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
                .message("Refund eligible as per policy.")
                .build();
    }

    private LocalDateTime getDepartureDateTime(Ticket ticket) {
        LocalTime departureTime = ticket.getTrain() != null && ticket.getTrain().getDepartureTime() != null
                ? ticket.getTrain().getDepartureTime()
                : LocalTime.MIDNIGHT;
        return ticket.getJourneyDate().atTime(departureTime);
    }
}
