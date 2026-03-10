package com.railease.service.impl.admin;

import com.railease.dto.admin.AdminTrainUpdateDTO;
import com.railease.entity.Train;
import com.railease.exception.TrainNotFoundException;
import com.railease.repository.TrainRepository;
import com.railease.service.admin.AdminTrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminTrainServiceImpl implements AdminTrainService {

    private final TrainRepository trainRepository;

    @Override
    public List<Train> getAllTrains() {
        log.debug("Admin: Fetching all trains");
        return trainRepository.findAll();
    }

    @Override
    public Train getTrainByNumber(Integer trainNo) throws TrainNotFoundException {
        log.debug("Admin: Fetching train by number: {}", trainNo);
        return trainRepository.findById(trainNo)
                .orElseThrow(() -> new TrainNotFoundException("Train not found with number: " + trainNo));
    }

    @Override
    public Train addTrain(AdminTrainUpdateDTO trainDTO) {
        log.info("Admin: Adding new train: {}", trainDTO.getTrainNo());

        if (trainRepository.existsById(trainDTO.getTrainNo())) {
            throw new RuntimeException("Train with number " + trainDTO.getTrainNo() + " already exists!");
        }

        Train train = Train.builder()
                .trainNo(trainDTO.getTrainNo())
                .trainName(trainDTO.getTrainName())
                .source(trainDTO.getSourceStation())
                .destination(trainDTO.getDestinationStation())
                .sourceStation(trainDTO.getSourceStation())
                .destinationStation(trainDTO.getDestinationStation())
                .departureTime(trainDTO.getDepartureTime())
                .arrivalTime(trainDTO.getArrivalTime())
                .travelDate(trainDTO.getJourneyDate())
                .journeyDate(trainDTO.getJourneyDate())
                .availableSeats(trainDTO.getAcSeats() + trainDTO.getSleeperSeats() + trainDTO.getGeneralSeats())
                .ticketPrice(trainDTO.getAcFare())
                .acSeats(trainDTO.getAcSeats())
                .sleeperSeats(trainDTO.getSleeperSeats())
                .generalSeats(trainDTO.getGeneralSeats())
                .acFare(trainDTO.getAcFare())
                .sleeperFare(trainDTO.getSleeperFare())
                .generalFare(trainDTO.getGeneralFare())
                .isActive(trainDTO.getIsActive())
                .build();

        return trainRepository.save(train);
    }

    @Override
    public Train updateTrain(AdminTrainUpdateDTO trainDTO) {
        log.info("Admin: Updating train: {}", trainDTO.getTrainNo());

        Train existingTrain = getTrainByNumber(trainDTO.getTrainNo());

        existingTrain.setTrainName(trainDTO.getTrainName());
        existingTrain.setSource(trainDTO.getSourceStation());
        existingTrain.setDestination(trainDTO.getDestinationStation());
        existingTrain.setSourceStation(trainDTO.getSourceStation());
        existingTrain.setDestinationStation(trainDTO.getDestinationStation());
        existingTrain.setDepartureTime(trainDTO.getDepartureTime());
        existingTrain.setArrivalTime(trainDTO.getArrivalTime());
        existingTrain.setTravelDate(trainDTO.getJourneyDate());
        existingTrain.setJourneyDate(trainDTO.getJourneyDate());
        existingTrain.setAvailableSeats(trainDTO.getAcSeats() + trainDTO.getSleeperSeats() + trainDTO.getGeneralSeats());
        existingTrain.setTicketPrice(trainDTO.getAcFare());
        existingTrain.setAcSeats(trainDTO.getAcSeats());
        existingTrain.setSleeperSeats(trainDTO.getSleeperSeats());
        existingTrain.setGeneralSeats(trainDTO.getGeneralSeats());
        existingTrain.setAcFare(trainDTO.getAcFare());
        existingTrain.setSleeperFare(trainDTO.getSleeperFare());
        existingTrain.setGeneralFare(trainDTO.getGeneralFare());
        existingTrain.setIsActive(trainDTO.getIsActive());

        return trainRepository.save(existingTrain);
    }

    @Override
    public Train updateTrainSeats(Integer trainNo, Integer acSeats, Integer sleeperSeats, Integer generalSeats) {
        log.info("Admin: Updating seats for train: {}", trainNo);

        Train train = getTrainByNumber(trainNo);

        if (acSeats != null) train.setAcSeats(acSeats);
        if (sleeperSeats != null) train.setSleeperSeats(sleeperSeats);
        if (generalSeats != null) train.setGeneralSeats(generalSeats);
        
        // Recalculate total available seats
        Integer totalSeats = (train.getAcSeats() != null ? train.getAcSeats() : 0) +
                           (train.getSleeperSeats() != null ? train.getSleeperSeats() : 0) +
                           (train.getGeneralSeats() != null ? train.getGeneralSeats() : 0);
        train.setAvailableSeats(totalSeats);

        return trainRepository.save(train);
    }

    @Override
    public Train updateTrainTimings(Integer trainNo, LocalTime departureTime, LocalTime arrivalTime) {
        log.info("Admin: Updating timings for train: {}", trainNo);

        Train train = getTrainByNumber(trainNo);

        if (departureTime != null) train.setDepartureTime(departureTime);
        if (arrivalTime != null) train.setArrivalTime(arrivalTime);

        return trainRepository.save(train);
    }

    @Override
    public boolean deleteTrain(Integer trainNo) {
        log.info("Admin: Deleting train: {}", trainNo);

        if (trainRepository.existsById(trainNo)) {
            trainRepository.deleteById(trainNo);
            return true;
        }
        throw new TrainNotFoundException("Train not found with number: " + trainNo);
    }

    @Override
    public boolean activateTrain(Integer trainNo) {
        log.info("Admin: Activating train: {}", trainNo);

        Train train = getTrainByNumber(trainNo);
        train.setIsActive(true);
        trainRepository.save(train);
        return true;
    }

    @Override
    public boolean deactivateTrain(Integer trainNo) {
        log.info("Admin: Deactivating train: {}", trainNo);

        Train train = getTrainByNumber(trainNo);
        train.setIsActive(false);
        trainRepository.save(train);
        return true;
    }
}