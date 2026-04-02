package com.railease.dto.admin;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AdminTrainUpdateDTO {

    @NotNull(message = "Train number is required")
    private Integer trainNo;

    @NotBlank(message = "Train name is required")
    @Size(min = 3, max = 50, message = "Train name must be between 3 and 50 characters")
    private String trainName;

    @NotBlank(message = "Source station is required")
    private String sourceStation;

    @NotBlank(message = "Destination station is required")
    private String destinationStation;

    private String sourceCode;

    private String destinationCode;

    @NotNull(message = "Departure time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime arrivalTime;

    @NotNull(message = "Journey date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate journeyDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate runFromDate;

    @NotNull(message = "AC seats count is required")
    @Min(value = 0, message = "AC seats cannot be negative")
    private Integer acSeats;

    @NotNull(message = "Sleeper seats count is required")
    @Min(value = 0, message = "Sleeper seats cannot be negative")
    private Integer sleeperSeats;

    @NotNull(message = "General seats count is required")
    @Min(value = 0, message = "General seats cannot be negative")
    private Integer generalSeats;

    @NotNull(message = "AC fare is required")
    @Positive(message = "AC fare must be positive")
    private Double acFare;

    @NotNull(message = "Sleeper fare is required")
    @Positive(message = "Sleeper fare must be positive")
    private Double sleeperFare;

    @NotNull(message = "General fare is required")
    @Positive(message = "General fare must be positive")
    private Double generalFare;

    private Boolean isActive = true;
}
