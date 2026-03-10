package com.railease.service.admin;

import com.railease.dto.admin.AdminTrainUpdateDTO;
import com.railease.entity.Train;
import com.railease.exception.TrainNotFoundException;
import java.time.LocalTime;
import java.util.List;

public interface AdminTrainService {

    List<Train> getAllTrains();

    Train getTrainByNumber(Integer trainNo) throws TrainNotFoundException;

    Train addTrain(AdminTrainUpdateDTO trainDTO);

    Train updateTrain(AdminTrainUpdateDTO trainDTO);

    Train updateTrainSeats(Integer trainNo, Integer acSeats, Integer sleeperSeats, Integer generalSeats);

    Train updateTrainTimings(Integer trainNo, LocalTime departureTime, LocalTime arrivalTime);

    boolean deleteTrain(Integer trainNo);

    boolean activateTrain(Integer trainNo);

    boolean deactivateTrain(Integer trainNo);
}