package com.railease.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus; // PENDING, PREPARING, DELIVERED, CANCELLED, REFUNDED

    @Column(name = "delivery_station", length = 50)
    private String deliveryStation;

    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "refund_status", length = 20)
    private String refundStatus; // PENDING, APPROVED, PROCESSED, REJECTED

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        deliveryStatus = "PENDING";
    }
}