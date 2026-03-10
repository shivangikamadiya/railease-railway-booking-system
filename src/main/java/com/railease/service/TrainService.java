package com.railease.service;

import com.railease.dto.TrainDTO;
import com.railease.dto.admin.AdminTrainUpdateDTO;
import com.railease.entity.Meal;
import com.railease.entity.Train;
import com.railease.exception.TrainNotFoundException;

import java.time.LocalDate;
import java.util.List;

public interface TrainService {

    List<Train> getAllTrains();

    List<Train> getAllActiveTrains();

    Train getTrainById(Integer id) throws TrainNotFoundException;

    Train getTrainByNumber(Integer trainNo) throws TrainNotFoundException;

    Train addTrain(AdminTrainUpdateDTO trainDTO);

    Train createTrain(TrainDTO trainDTO);

    Train updateTrain(AdminTrainUpdateDTO trainDTO);

    Train updateTrain(Train train);

    Train updateTrain(Integer id, TrainDTO trainDTO) throws TrainNotFoundException;

    void deleteTrain(Integer trainNo);

    boolean existsByTrainNo(Integer trainNo);

    List<Train> findTrainsBetweenStations(String source, String destination, LocalDate journeyDate);

    List<Train> searchTrains(String keyword);

    int getAvailableSeats(Integer trainNo, String classType, LocalDate journeyDate);

    Train activateTrain(Integer trainNo);

    Train deactivateTrain(Integer trainNo);

    Train toggleTrainStatus(Integer id) throws TrainNotFoundException;

    List<Meal> getMealsByTrain(Integer trainNo);

    Train assignMealToTrain(Integer trainNo, Long mealId);

    Train removeMealFromTrain(Integer trainNo, Long mealId);
}