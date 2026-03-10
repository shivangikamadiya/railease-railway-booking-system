package com.railease.service;

import com.railease.constants.TicketStatus;
import com.railease.dto.*;
import com.railease.entity.Ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface BookingService {

    // ==================== BASIC BOOKING OPERATIONS ====================

    Ticket createTicket(Long userId, Integer trainNo, String passengerName,
                        Integer passengerAge, String passengerGender,
                        String classType, Integer numberOfSeats, LocalDate journeyDate);

    Ticket confirmBooking(String ticketId, String paymentId, String paymentMethod);

    Ticket getTicketById(String ticketId);

    List<Ticket> getUserBookings(Long userId);

    List<Ticket> getUserRecentBookings(Long userId, int limit);

    List<Ticket> getUserActiveBookings(Long userId);

    List<Ticket> getUserPastBookings(Long userId);

    List<Ticket> getUserCancelledBookings(Long userId);

    List<Ticket> getUserRefundedBookings(Long userId);

    Long getUserBookingCount(Long userId);

    Long getUserUpcomingJourneysCount(Long userId);

    // ==================== NEW METHODS NEEDED ====================

    /**
     * Get total amount spent by a user
     */
    Double getTotalSpentByUser(Long userId);

    /**
     * Get available seats for a specific train, class and date
     */
    int getAvailableSeats(Integer trainNo, String classType, LocalDate journeyDate);

    // ==================== CANCELLATION & REFUND METHODS ====================

    CancellationResponseDTO initiateCancellation(String ticketId, Long userId, CancellationRequestDTO request);

    CancellationResponseDTO processCancellation(String ticketId, Long userId, String reason);

    CancellationResponseDTO processRefund(String ticketId, Long userId);

    RefundEstimateDTO calculateRefundEstimate(String ticketId, Long userId);

    RefundStatusDTO checkRefundStatus(String ticketId, Long userId);

    List<RefundStatusDTO> getUserRefunds(Long userId);

    Map<String, Object> getRefundStatistics(Long userId);

    Map<String, Long> getUserRefundStats(Long userId);

    boolean isCancellable(String ticketId);

    RefundCalculationDTO calculateRefund(Double totalFare, LocalDate journeyDate, LocalDateTime bookingDate);

    CancellationPolicyDTO getCancellationPolicy();

    boolean checkSeatAvailability(Integer trainNo, String classType,
                                  Integer numberOfSeats, LocalDate journeyDate);

    Long getBookedSeatsCount(Integer trainNo, LocalDate journeyDate, String classType);

    List<Ticket> getTicketsByStatus(TicketStatus status);

    Ticket updateTicketStatus(String ticketId, TicketStatus status);

    void cancelTicket(String ticketId, Long userId, String reason);

    Double calculateRefundAmount(Double totalFare, LocalDate journeyDate);
}