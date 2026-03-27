package com.railease.repository;

import com.railease.constants.TicketStatus;
import com.railease.entity.Ticket;
import com.railease.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    // Fetch ticket with train and user eagerly loaded
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train LEFT JOIN FETCH t.user WHERE t.ticketId = :ticketId")
    Optional<Ticket> findByIdWithDetails(@Param("ticketId") String ticketId);

    // Basic user queries - with eager loading of Train
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train WHERE t.user = :user ORDER BY t.bookingDate DESC")
    List<Ticket> findByUserOrderByBookingDateDesc(@Param("user") User user);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train WHERE t.user = :user ORDER BY t.bookingDate DESC")
    List<Ticket> findRecentByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // Status-based queries
    @Query("SELECT t FROM Ticket t WHERE t.ticketStatus = :status")
    List<Ticket> findByTicketStatus(@Param("status") TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE t.user.userId = :userId AND t.ticketStatus = :status")
    List<Ticket> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TicketStatus status);

    // Date-based queries - with eager loading of Train
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train " +
            "WHERE t.user.userId = :userId " +
            "AND t.journeyDate >= CURRENT_DATE " +
            "AND t.ticketStatus = 'CONFIRMED' " +
            "AND t.bookingStatus = 'CONFIRMED'")
    List<Ticket> findActiveTicketsByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train WHERE t.user.userId = :userId AND t.journeyDate < CURRENT_DATE")
    List<Ticket> findPastTicketsByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train WHERE t.user.userId = :userId AND t.ticketStatus = 'CANCELLED'")
    List<Ticket> findCancelledTicketsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.userId = :userId " +
            "AND t.journeyDate >= CURRENT_DATE " +
            "AND t.ticketStatus = 'CONFIRMED' " +
            "AND t.bookingStatus = 'CONFIRMED'")
    Long countUpcomingByUserId(@Param("userId") Long userId);

    // Seat availability queries
    @Query("SELECT COALESCE(SUM(t.numberOfSeats), 0) FROM Ticket t " +
            "WHERE t.train.trainNo = :trainNo " +
            "AND t.journeyDate = :journeyDate " +
            "AND t.classType = :classType " +
            "AND t.ticketStatus != 'CANCELLED'")
    Long countBookedSeatsByTrainAndDateAndClass(@Param("trainNo") Integer trainNo,
                                                @Param("journeyDate") LocalDate journeyDate,
                                                @Param("classType") String classType);

    // Train statistics
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.train.trainNo = :trainNo")
    Long countByTrainNo(@Param("trainNo") Integer trainNo);

    // Refund-related queries - with eager loading of Train
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train WHERE t.user.userId = :userId AND t.refundStatus IS NOT NULL")
    List<Ticket> findTicketsWithRefundByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.train WHERE t.user.userId = :userId AND t.refundStatus = :status")
    List<Ticket> findByUserIdAndRefundStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.userId = :userId AND t.refundStatus = 'PENDING'")
    Long countPendingRefundsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.userId = :userId AND t.refundStatus = 'PROCESSING'")
    Long countProcessingRefundsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.userId = :userId AND t.refundStatus = 'COMPLETED'")
    Long countCompletedRefundsByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.train " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.cancellationRequestedDate IS NOT NULL AND t.refundStatus = 'PENDING' " +
            "ORDER BY t.cancellationRequestedDate DESC")
    List<Ticket> findCancellationRequests();

    @Query("SELECT t FROM Ticket t WHERE t.refundStatus = :status")
    List<Ticket> findByRefundStatus(@Param("status") String status);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.train " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.cancellationRequestedDate IS NOT NULL " +
            "ORDER BY t.cancellationRequestedDate DESC")
    List<Ticket> findAllCancellationHistory();
}
