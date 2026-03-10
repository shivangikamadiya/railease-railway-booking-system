package com.railease.service;

import com.railease.dto.MealDTO;
import com.railease.entity.Meal;
import com.railease.entity.MealOrder;
import com.railease.exception.MealNotFoundException;

import java.util.List;

public interface MealService {

    // Basic CRUD operations
    List<Meal> getAllMeals();

    List<Meal> getAvailableMeals();

    Meal getMealById(Long id) throws MealNotFoundException;

    Meal createMeal(MealDTO mealDTO);

    Meal updateMeal(Long id, MealDTO mealDTO) throws MealNotFoundException;

    void deleteMeal(Long id) throws MealNotFoundException;

    Meal toggleMealAvailability(Long id) throws MealNotFoundException;

    // Search operations
    List<Meal> searchMeals(String keyword);

    List<Meal> getMealsByType(String mealType);

    // Train specific meals - FIXED METHOD SIGNATURE
    List<Meal> getMealsByTrain(Integer trainNo);

    // Meal order operations
    MealOrder orderMeal(Long userId, String ticketId, Long mealId, Integer quantity,
                        String deliveryStation, String instructions);

    List<MealOrder> getUserMealOrders(Long userId);

    MealOrder getMealOrderById(Long orderId);

    void cancelMealOrder(Long orderId, Long userId);

    // Admin operations
    List<MealOrder> getAllMealOrders();

    List<MealOrder> getMealOrdersByStatus(String status);

    List<MealOrder> getMealCancellationRequests();

    MealOrder approveMealCancellation(Long orderId, Double refundPercentage);

    MealOrder rejectMealCancellation(Long orderId, String reason);

    Double calculateMealRefund(Double totalPrice, java.time.LocalDateTime orderDate,
                               java.time.LocalDateTime cancellationTime);
}