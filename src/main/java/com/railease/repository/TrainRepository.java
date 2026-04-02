package com.railease.repository;

import com.railease.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainRepository extends JpaRepository<Train, Integer> {

    List<Train> findByIsActiveTrue();

    List<Train> findBySourceAndDestinationAndTravelDateAndIsActiveTrue(
            String source, String destination, LocalDate travelDate);

    @Query("SELECT t FROM Train t " +
            "WHERE t.isActive = true " +
            "AND :journeyDate BETWEEN COALESCE(t.runFromDate, CURRENT_DATE) " +
            "AND COALESCE(t.runToDate, t.travelDate, t.journeyDate) " +
            "AND (" +
            "LOWER(TRIM(COALESCE(t.source, ''))) LIKE LOWER(CONCAT('%', TRIM(:source), '%')) " +
            "OR LOWER(TRIM(COALESCE(t.sourceStation, ''))) LIKE LOWER(CONCAT('%', TRIM(:source), '%')) " +
            "OR LOWER(TRIM(COALESCE(t.sourceCode, ''))) LIKE LOWER(CONCAT('%', TRIM(:source), '%'))" +
            ") " +
            "AND (" +
            "LOWER(TRIM(COALESCE(t.destination, ''))) LIKE LOWER(CONCAT('%', TRIM(:destination), '%')) " +
            "OR LOWER(TRIM(COALESCE(t.destinationStation, ''))) LIKE LOWER(CONCAT('%', TRIM(:destination), '%')) " +
            "OR LOWER(TRIM(COALESCE(t.destinationCode, ''))) LIKE LOWER(CONCAT('%', TRIM(:destination), '%'))" +
            ")")
    List<Train> findActiveTrainsByRouteAndDate(@Param("source") String source,
                                               @Param("destination") String destination,
                                               @Param("journeyDate") LocalDate journeyDate);

    @Query("SELECT t FROM Train t WHERE " +
            "LOWER(t.trainName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.source) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.destination) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Train> searchTrains(@Param("keyword") String keyword);

    boolean existsByTrainNameAndTravelDate(String trainName, LocalDate travelDate);
}
