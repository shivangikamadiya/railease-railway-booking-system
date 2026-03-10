package com.railease.repository;

import com.railease.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByAvailabilityStatusTrue();

    List<Meal> findByMealType(String mealType);

    @Query("SELECT m FROM Meal m WHERE " +
            "LOWER(m.mealName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Meal> searchMeals(@Param("keyword") String keyword);

    boolean existsByMealName(String mealName);

    @Query("SELECT m FROM Meal m JOIN m.trains t WHERE t.id = :trainNo")
    List<Meal> findMealsByTrainNo(@Param("trainNo") Integer trainNo);
}