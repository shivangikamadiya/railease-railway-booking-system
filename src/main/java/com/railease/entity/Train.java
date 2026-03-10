package com.railease.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "train_no", nullable = false)
    private Integer trainNo;

    @NotBlank(message = "Train name is required")
    @Column(name = "train_name", nullable = false, length = 100)
    private String trainName;

    @NotBlank(message = "Source station is required")
    @Column(name = "source_station", nullable = false, length = 100)
    private String sourceStation;

    @NotBlank(message = "Destination station is required")
    @Column(name = "destination_station", nullable = false, length = 100)
    private String destinationStation;

    @NotNull(message = "Departure time is required")
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "destination", length = 100)
    private String destination;

    @NotNull(message = "Journey date is required")
    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(name = "available_seats")
    private Integer availableSeats;

    @Column(name = "ticket_price")
    private Double ticketPrice;

    @Column(name = "ac_seats")
    private Integer acSeats;

    @Column(name = "sleeper_seats")
    private Integer sleeperSeats;

    @Column(name = "general_seats")
    private Integer generalSeats;

    @Column(name = "ac_fare")
    private Double acFare;

    @Column(name = "sleeper_fare")
    private Double sleeperFare;

    @Column(name = "general_fare")
    private Double generalFare;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
        name = "train_meals",
        joinColumns = @JoinColumn(name = "train_no"),
        inverseJoinColumns = @JoinColumn(name = "meal_id")
    )
    @Builder.Default
    private List<Meal> availableMeals = new ArrayList<>();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        // Map source/destination to station columns for consistency
        if (source != null && sourceStation == null) {
            sourceStation = source;
        }
        if (destination != null && destinationStation == null) {
            destinationStation = destination;
        }
        if (travelDate != null && journeyDate == null) {
            journeyDate = travelDate;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}