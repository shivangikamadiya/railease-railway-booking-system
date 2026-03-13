package com.railease.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
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
    @Column(name = "train_no")
    private Integer trainNo;

    @Column(name = "train_name", nullable = false, length = 100)
    private String trainName;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "destination", length = 50)
    private String destination;

    @Column(name = "source_station", nullable = false, length = 50)
    private String sourceStation;

    @Column(name = "destination_station", nullable = false, length = 50)
    private String destinationStation;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(name = "journey_date")
    private LocalDate journeyDate;

    @Column(name = "available_seats")
    private Integer availableSeats;

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

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "id")
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "train_meals",
            joinColumns = @JoinColumn(name = "train_no"),
            inverseJoinColumns = @JoinColumn(name = "meal_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Meal> availableMeals = new ArrayList<>();

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Ticket> tickets = new ArrayList<>();
}
