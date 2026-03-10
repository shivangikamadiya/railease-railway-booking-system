package com.railease.service.impl;

import com.railease.dto.TrainDTO;
import com.railease.dto.admin.AdminTrainUpdateDTO;
import com.railease.entity.Meal;
import com.railease.entity.Train;
import com.railease.exception.TrainNotFoundException;
import com.railease.repository.MealRepository;
import com.railease.repository.TicketRepository;
import com.railease.repository.TrainRepository;
import com.railease.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;
    private final MealRepository mealRepository;
    private final TicketRepository ticketRepository;

    @Override
    public List<Train> getAllTrains() {
        log.debug("Fetching all trains");
        return trainRepository.findAll();
    }

    @Override
    @Cacheable(value = "activeTrains")
    public List<Train> getAllActiveTrains() {
        log.debug("Fetching all active trains");
        return trainRepository.findByIsActiveTrue();
    }

    @Override
    public Train getTrainById(Integer id) throws TrainNotFoundException {
        log.debug("Fetching train by id: {}", id);
        return trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException("Train not found with id: " + id));
    }

    @Override
    public Train getTrainByNumber(Integer trainNo) throws TrainNotFoundException {
        log.debug("Fetching train by number: {}", trainNo);
        return trainRepository.findById(trainNo)
                .orElseThrow(() -> new TrainNotFoundException("Train not found with train number: " + trainNo));
    }

    @Override
    public Train addTrain(AdminTrainUpdateDTO trainDTO) {
        log.info("Adding new train: {}", trainDTO.getTrainNo());

        if (trainRepository.existsById(trainDTO.getTrainNo())) {
            throw new RuntimeException("Train with number " + trainDTO.getTrainNo() + " already exists!");
        }

        int totalSeats = (trainDTO.getAcSeats() != null ? trainDTO.getAcSeats() : 0) +
                (trainDTO.getSleeperSeats() != null ? trainDTO.getSleeperSeats() : 0) +
                (trainDTO.getGeneralSeats() != null ? trainDTO.getGeneralSeats() : 0);

        Train train = Train.builder()
                .trainNo(trainDTO.getTrainNo())
                .trainName(trainDTO.getTrainName())
                .sourceStation(trainDTO.getSourceStation())
                .destinationStation(trainDTO.getDestinationStation())
                .source(trainDTO.getSourceStation())
                .destination(trainDTO.getDestinationStation())
                .departureTime(trainDTO.getDepartureTime())
                .arrivalTime(trainDTO.getArrivalTime())
                .journeyDate(trainDTO.getJourneyDate())
                .travelDate(trainDTO.getJourneyDate())
                .availableSeats(totalSeats)
                .ticketPrice(trainDTO.getAcFare())
                .acSeats(trainDTO.getAcSeats())
                .sleeperSeats(trainDTO.getSleeperSeats())
                .generalSeats(trainDTO.getGeneralSeats())
                .acFare(trainDTO.getAcFare())
                .sleeperFare(trainDTO.getSleeperFare())
                .generalFare(trainDTO.getGeneralFare())
                .isActive(trainDTO.getIsActive() != null ? trainDTO.getIsActive() : true)
                .build();

        Train savedTrain = trainRepository.save(train);
        
        log.info("Train added successfully - acSeats: {}, sleeperSeats: {}, generalSeats: {}, acFare: {}, sleeperFare: {}, generalFare: {}",
                savedTrain.getAcSeats(), savedTrain.getSleeperSeats(), savedTrain.getGeneralSeats(),
                savedTrain.getAcFare(), savedTrain.getSleeperFare(), savedTrain.getGeneralFare());
        
        return savedTrain;
    }

    @Override
    public Train createTrain(TrainDTO trainDTO) {
        log.info("Creating new train: {}", trainDTO.getTrainName());

        if (trainDTO.getTrainNo() != null && trainRepository.existsById(trainDTO.getTrainNo())) {
            throw new RuntimeException("Train with number " + trainDTO.getTrainNo() + " already exists!");
        }

        int totalSeats = (trainDTO.getAcSeats() != null ? trainDTO.getAcSeats() : 0) +
                (trainDTO.getSleeperSeats() != null ? trainDTO.getSleeperSeats() : 0) +
                (trainDTO.getGeneralSeats() != null ? trainDTO.getGeneralSeats() : 0);

        Train train = Train.builder()
                .trainNo(trainDTO.getTrainNo())
                .trainName(trainDTO.getTrainName())
                .sourceStation(trainDTO.getSource())
                .destinationStation(trainDTO.getDestination())
                .source(trainDTO.getSource())
                .destination(trainDTO.getDestination())
                .departureTime(trainDTO.getDepartureTime())
                .arrivalTime(trainDTO.getArrivalTime())
                .journeyDate(trainDTO.getTravelDate())
                .travelDate(trainDTO.getTravelDate())
                .availableSeats(totalSeats)
                .ticketPrice(trainDTO.getTicketPrice())
                .acSeats(trainDTO.getAcSeats())
                .sleeperSeats(trainDTO.getSleeperSeats())
                .generalSeats(trainDTO.getGeneralSeats())
                .acFare(trainDTO.getAcFare())
                .sleeperFare(trainDTO.getSleeperFare())
                .generalFare(trainDTO.getGeneralFare())
                .isActive(trainDTO.getIsActive() != null ? trainDTO.getIsActive() : true)
                .build();

        return trainRepository.save(train);
    }

    @Override
    public Train updateTrain(AdminTrainUpdateDTO trainDTO) {
        log.info("Updating train: {}", trainDTO.getTrainNo());

        Train existingTrain = getTrainByNumber(trainDTO.getTrainNo());

        if (trainDTO.getTrainName() != null) {
            existingTrain.setTrainName(trainDTO.getTrainName());
        }
        if (trainDTO.getSourceStation() != null) {
            existingTrain.setSourceStation(trainDTO.getSourceStation());
            existingTrain.setSource(trainDTO.getSourceStation());
        }
        if (trainDTO.getDestinationStation() != null) {
            existingTrain.setDestinationStation(trainDTO.getDestinationStation());
            existingTrain.setDestination(trainDTO.getDestinationStation());
        }
        if (trainDTO.getDepartureTime() != null) {
            existingTrain.setDepartureTime(trainDTO.getDepartureTime());
        }
        if (trainDTO.getArrivalTime() != null) {
            existingTrain.setArrivalTime(trainDTO.getArrivalTime());
        }
        if (trainDTO.getJourneyDate() != null) {
            existingTrain.setJourneyDate(trainDTO.getJourneyDate());
            existingTrain.setTravelDate(trainDTO.getJourneyDate());
        }
        if (trainDTO.getAcSeats() != null) {
            existingTrain.setAcSeats(trainDTO.getAcSeats());
        }
        if (trainDTO.getSleeperSeats() != null) {
            existingTrain.setSleeperSeats(trainDTO.getSleeperSeats());
        }
        if (trainDTO.getGeneralSeats() != null) {
            existingTrain.setGeneralSeats(trainDTO.getGeneralSeats());
        }
        if (trainDTO.getAcFare() != null) {
            existingTrain.setAcFare(trainDTO.getAcFare());
            existingTrain.setTicketPrice(trainDTO.getAcFare());
        }
        if (trainDTO.getSleeperFare() != null) {
            existingTrain.setSleeperFare(trainDTO.getSleeperFare());
        }
        if (trainDTO.getGeneralFare() != null) {
            existingTrain.setGeneralFare(trainDTO.getGeneralFare());
        }
        if (trainDTO.getIsActive() != null) {
            existingTrain.setIsActive(trainDTO.getIsActive());
        }

        int totalSeats = (existingTrain.getAcSeats() != null ? existingTrain.getAcSeats() : 0) +
                (existingTrain.getSleeperSeats() != null ? existingTrain.getSleeperSeats() : 0) +
                (existingTrain.getGeneralSeats() != null ? existingTrain.getGeneralSeats() : 0);
        existingTrain.setAvailableSeats(totalSeats);

        Train savedTrain = trainRepository.save(existingTrain);

        log.info("Train updated successfully - acSeats: {}, sleeperSeats: {}, generalSeats: {}, acFare: {}, sleeperFare: {}, generalFare: {}",
                savedTrain.getAcSeats(), savedTrain.getSleeperSeats(), savedTrain.getGeneralSeats(),
                savedTrain.getAcFare(), savedTrain.getSleeperFare(), savedTrain.getGeneralFare());

        return savedTrain;
    }

    @Override
    public Train updateTrain(Train train) {
        log.info("Updating train: {}", train.getTrainNo());
        return trainRepository.save(train);
    }

    @Override
    public Train updateTrain(Integer id, TrainDTO trainDTO) throws TrainNotFoundException {
        log.info("Updating train with id: {}", id);

        Train existingTrain = getTrainById(id);

        if (trainDTO.getTrainName() != null) {
            existingTrain.setTrainName(trainDTO.getTrainName());
        }
        if (trainDTO.getSource() != null) {
            existingTrain.setSourceStation(trainDTO.getSource());
            existingTrain.setSource(trainDTO.getSource());
        }
        if (trainDTO.getDestination() != null) {
            existingTrain.setDestinationStation(trainDTO.getDestination());
            existingTrain.setDestination(trainDTO.getDestination());
        }
        if (trainDTO.getDepartureTime() != null) {
            existingTrain.setDepartureTime(trainDTO.getDepartureTime());
        }
        if (trainDTO.getArrivalTime() != null) {
            existingTrain.setArrivalTime(trainDTO.getArrivalTime());
        }
        if (trainDTO.getTravelDate() != null) {
            existingTrain.setJourneyDate(trainDTO.getTravelDate());
            existingTrain.setTravelDate(trainDTO.getTravelDate());
        }
        if (trainDTO.getAcSeats() != null) {
            existingTrain.setAcSeats(trainDTO.getAcSeats());
        }
        if (trainDTO.getSleeperSeats() != null) {
            existingTrain.setSleeperSeats(trainDTO.getSleeperSeats());
        }
        if (trainDTO.getGeneralSeats() != null) {
            existingTrain.setGeneralSeats(trainDTO.getGeneralSeats());
        }
        if (trainDTO.getAcFare() != null) {
            existingTrain.setAcFare(trainDTO.getAcFare());
        }
        if (trainDTO.getSleeperFare() != null) {
            existingTrain.setSleeperFare(trainDTO.getSleeperFare());
        }
        if (trainDTO.getGeneralFare() != null) {
            existingTrain.setGeneralFare(trainDTO.getGeneralFare());
        }
        if (trainDTO.getTicketPrice() != null) {
            existingTrain.setTicketPrice(trainDTO.getTicketPrice());
        }
        if (trainDTO.getIsActive() != null) {
            existingTrain.setIsActive(trainDTO.getIsActive());
        }

        int totalSeats = (existingTrain.getAcSeats() != null ? existingTrain.getAcSeats() : 0) +
                (existingTrain.getSleeperSeats() != null ? existingTrain.getSleeperSeats() : 0) +
                (existingTrain.getGeneralSeats() != null ? existingTrain.getGeneralSeats() : 0);
        existingTrain.setAvailableSeats(totalSeats);

        return trainRepository.save(existingTrain);
    }

    @Override
    public void deleteTrain(Integer trainNo) {
        log.info("Deleting train: {}", trainNo);

        if (!trainRepository.existsById(trainNo)) {
            throw new TrainNotFoundException("Train not found with number: " + trainNo);
        }

        Long ticketCount = ticketRepository.countByTrainNo(trainNo);

        if (ticketCount > 0) {
            Train train = getTrainByNumber(trainNo);
            train.setIsActive(false);
            trainRepository.save(train);
            log.info("Train deactivated instead of deleted due to {} existing tickets", ticketCount);
        } else {
            trainRepository.deleteById(trainNo);
            log.info("Train deleted successfully: {}", trainNo);
        }
    }

    @Override
    public boolean existsByTrainNo(Integer trainNo) {
        return trainRepository.existsById(trainNo);
    }

    @Override
    public List<Train> findTrainsBetweenStations(String source, String destination, LocalDate journeyDate) {
        log.info("Searching trains from {} to {} on {}", source, destination, journeyDate);
        return trainRepository.findBySourceAndDestinationAndTravelDateAndIsActiveTrue(
                source, destination, journeyDate);
    }

    @Override
    public List<Train> searchTrains(String keyword) {
        log.info("Searching trains with keyword: {}", keyword);
        return trainRepository.searchTrains(keyword);
    }

    @Override
    public int getAvailableSeats(Integer trainNo, String classType, LocalDate journeyDate) {
        Train train = getTrainByNumber(trainNo);
        
        int totalSeats;
        if (classType != null) {
            switch (classType.toUpperCase()) {
                case "AC":
                    totalSeats = train.getAcSeats() != null ? train.getAcSeats() : 0;
                    break;
                case "SLEEPER":
                    totalSeats = train.getSleeperSeats() != null ? train.getSleeperSeats() : 0;
                    break;
                case "GENERAL":
                    totalSeats = train.getGeneralSeats() != null ? train.getGeneralSeats() : 0;
                    break;
                default:
                    totalSeats = train.getAvailableSeats() != null ? train.getAvailableSeats() : 0;
            }
        } else {
            totalSeats = train.getAvailableSeats() != null ? train.getAvailableSeats() : 0;
        }

        Long bookedSeats = ticketRepository.countBookedSeatsByTrainAndDateAndClass(trainNo, journeyDate, classType);

        return Math.max(0, totalSeats - (bookedSeats != null ? bookedSeats.intValue() : 0));
    }

    @Override
    public Train activateTrain(Integer trainNo) {
        log.info("Activating train: {}", trainNo);
        Train train = getTrainByNumber(trainNo);
        train.setIsActive(true);
        return trainRepository.save(train);
    }

    @Override
    public Train deactivateTrain(Integer trainNo) {
        log.info("Deactivating train: {}", trainNo);
        Train train = getTrainByNumber(trainNo);
        train.setIsActive(false);
        return trainRepository.save(train);
    }

    @Override
    public Train toggleTrainStatus(Integer id) throws TrainNotFoundException {
        log.info("Toggling status for train with id: {}", id);

        Train train = getTrainById(id);
        train.setIsActive(!train.getIsActive());

        return trainRepository.save(train);
    }

    @Override
    public List<Meal> getMealsByTrain(Integer trainNo) {
        log.info("Fetching meals for train: {}", trainNo);
        Train train = getTrainByNumber(trainNo);
        return train.getAvailableMeals();
    }

    @Override
    public Train assignMealToTrain(Integer trainNo, Long mealId) {
        log.info("Assigning meal {} to train {}", mealId, trainNo);

        Train train = getTrainByNumber(trainNo);
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new RuntimeException("Meal not found with id: " + mealId));

        if (!train.getAvailableMeals().contains(meal)) {
            train.getAvailableMeals().add(meal);
            trainRepository.save(train);
        }

        return train;
    }

    @Override
    public Train removeMealFromTrain(Integer trainNo, Long mealId) {
        log.info("Removing meal {} from train {}", mealId, trainNo);

        Train train = getTrainByNumber(trainNo);
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new RuntimeException("Meal not found with id: " + mealId));

        train.getAvailableMeals().remove(meal);
        trainRepository.save(train);

        return train;
    }
}
