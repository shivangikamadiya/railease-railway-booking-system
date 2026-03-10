package com.railease.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TrainDTO {

    private Integer id;

    @NotNull(message = "Train number is required")
    @Positive(message = "Train number must be positive")
    private Integer trainNo;

    @NotBlank(message = "Train name is required")
    @Size(min = 3, max = 100, message = "Train name must be between 3 and 100 characters")
    private String trainName;

    @NotBlank(message = "Source station is required")
    private String source;

    @NotBlank(message = "Destination station is required")
    private String destination;

    @NotNull(message = "Departure time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime arrivalTime;

    @NotNull(message = "Travel date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    // Available seats - auto-calculated from AC + Sleeper + General seats
    private Integer availableSeats;

    // Ticket price - auto-set to AC fare
    private Double ticketPrice;

    // AC Seats - validation handled by controller/service
    private Integer acSeats;

    // Sleeper Seats
    private Integer sleeperSeats;

    // General Seats
    private Integer generalSeats;

    // AC Fare
    private Double acFare;

    // Sleeper Fare
    private Double sleeperFare;

    // General Fare
    private Double generalFare;

    private Boolean isActive = true;
}