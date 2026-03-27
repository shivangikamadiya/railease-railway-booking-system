package com.railease.entity;

import com.railease.constants.TicketStatus;
import lombok.*;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @Column(name = "ticket_id", unique = true, nullable = false, length = 20)
    private String ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_no", nullable = false)
    private Train train;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Column(name = "source_station", nullable = false, length = 50)
    private String sourceStation;

    @Column(name = "destination_station", nullable = false, length = 50)
    private String destinationStation;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "passenger_age", nullable = false)
    private Integer passengerAge;

    @Column(name = "passenger_gender", nullable = false, length = 10)
    private String passengerGender;

    @Column(name = "class_type", nullable = false, length = 20)
    private String classType;

    @Column(name = "number_of_seats", nullable = false)
    private Integer numberOfSeats;

    @Column(name = "total_fare", nullable = false)
    private Double totalFare;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false, length = 20)
    private TicketStatus ticketStatus;

    @Column(name = "booking_status", nullable = false, length = 20)
    private String bookingStatus;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Column(name = "payment_id", length = 50)
    private String paymentId;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "admin_remarks", length = 500)
    private String adminRemarks;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "refund_percentage")
    private Double refundPercentage;

    @Column(name = "refund_status", length = 20)
    private String refundStatus; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "refund_processed_date")
    private LocalDateTime refundProcessedDate;

    @Column(name = "cancellation_requested_date")
    private LocalDateTime cancellationRequestedDate;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "refund_transaction_id", length = 50)
    private String refundTransactionId;

    @Column(name = "cancellation_decision_date")
    private LocalDateTime cancellationDecisionDate;

    @Column(name = "cancellation_charges")
    private Double cancellationCharges;

    @Column(name = "meal_id")
    private Long mealId;

    @Column(name = "meal_quantity")
    private Integer mealQuantity;

    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
    }
}
