package com.railease.service.impl;

import com.railease.constants.TicketStatus;
import com.railease.dto.MealDTO;
import com.railease.entity.Meal;
import com.railease.entity.MealOrder;
import com.railease.entity.Ticket;
import com.railease.entity.User;
import com.railease.exception.MealNotFoundException;
import com.railease.repository.MealOrderRepository;
import com.railease.repository.MealRepository;
import com.railease.repository.TicketRepository;
import com.railease.repository.UserRepository;
import com.railease.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MealServiceImpl implements MealService {

    private final MealRepository mealRepository;
    private final MealOrderRepository mealOrderRepository;
    private final UserRepository userRepository;  // Added missing repository
    private final TicketRepository ticketRepository;  // Added missing repository

    @Override
    public List<Meal> getAllMeals() {
        log.info("Fetching all meals");
        return mealRepository.findAll();
    }

    @Override
    public List<Meal> getAvailableMeals() {
        log.info("Fetching available meals");
        return mealRepository.findByAvailabilityStatusTrue();
    }

    @Override
    public Meal getMealById(Long id) throws MealNotFoundException {
        log.info("Fetching meal with id: {}", id);
        return mealRepository.findById(id)
                .orElseThrow(() -> new MealNotFoundException("Meal not found with id: " + id));
    }

    @Override
    public Meal createMeal(MealDTO mealDTO) {
        log.info("Creating new meal: {}", mealDTO.getMealName());

        Meal meal = Meal.builder()
                .mealName(mealDTO.getMealName())
                .description(mealDTO.getDescription())
                .price(mealDTO.getPrice())
                .availabilityStatus(mealDTO.getAvailabilityStatus() != null ?
                        mealDTO.getAvailabilityStatus() : true)
                .mealType(normalizeMealType(mealDTO.getMealType()))
                .preparationTime(mealDTO.getPreparationTime())
                .build();

        applyMealImage(meal, mealDTO.getMealImage());

        Meal savedMeal = mealRepository.save(meal);
        log.info("Meal created successfully with id: {}", savedMeal.getId());
        return savedMeal;
    }

    @Override
    public Meal updateMeal(Long id, MealDTO mealDTO) throws MealNotFoundException {
        log.info("Updating meal with id: {}", id);

        Meal existingMeal = getMealById(id);

        existingMeal.setMealName(mealDTO.getMealName());
        existingMeal.setDescription(mealDTO.getDescription());
        existingMeal.setPrice(mealDTO.getPrice());
        existingMeal.setAvailabilityStatus(mealDTO.getAvailabilityStatus());
        existingMeal.setMealType(normalizeMealType(mealDTO.getMealType()));
        existingMeal.setPreparationTime(mealDTO.getPreparationTime());
        applyMealImage(existingMeal, mealDTO.getMealImage());

        Meal updatedMeal = mealRepository.save(existingMeal);
        log.info("Meal updated successfully with id: {}", updatedMeal.getId());
        return updatedMeal;
    }

    private void applyMealImage(Meal meal, MultipartFile mealImage) {
        if (mealImage == null || mealImage.isEmpty()) {
            return;
        }

        try {
            meal.setImage(mealImage.getBytes());
            meal.setImageContentType(mealImage.getContentType());
            meal.setImageUrl(mealImage.getOriginalFilename());
        } catch (IOException e) {
            log.error("Failed to read meal image for {}", meal.getMealName(), e);
            throw new RuntimeException("Unable to upload meal image", e);
        }
    }

    private String normalizeMealType(String mealType) {
        if (mealType == null) {
            return null;
        }

        String normalized = mealType.trim().toUpperCase();
        if ("SNACKS".equals(normalized)) {
            return "SNACK";
        }
        if ("BEVERAGES".equals(normalized)) {
            return "BEVERAGE";
        }
        return normalized;
    }

    @Override
    public void deleteMeal(Long id) throws MealNotFoundException {
        log.info("Deleting meal with id: {}", id);

        Meal meal = getMealById(id);
        mealRepository.delete(meal);
        log.info("Meal deleted successfully: {}", id);
    }

    @Override
    public Meal toggleMealAvailability(Long id) throws MealNotFoundException {
        log.info("Toggling availability for meal with id: {}", id);

        Meal meal = getMealById(id);
        meal.setAvailabilityStatus(!meal.getAvailabilityStatus());

        Meal updatedMeal = mealRepository.save(meal);
        log.info("Meal availability toggled to: {} for id: {}",
                updatedMeal.getAvailabilityStatus(), id);
        return updatedMeal;
    }

    @Override
    public List<Meal> searchMeals(String keyword) {
        log.info("Searching meals with keyword: {}", keyword);
        return mealRepository.searchMeals(keyword);
    }

    @Override
    public List<Meal> getMealsByType(String mealType) {
        log.info("Fetching meals by type: {}", mealType);
        return mealRepository.findByMealType(mealType);
    }

    @Override
    public List<Meal> getMealsByTrain(Integer trainNo) {
        log.info("Fetching meals for train: {}", trainNo);
        List<Meal> meals = mealRepository.findMealsByTrainNo(trainNo);
        if (meals.isEmpty()) {
            log.warn("No train-specific meals mapped for train {}. Falling back to available meals.", trainNo);
            return mealRepository.findByAvailabilityStatusTrue();
        }
        return meals;
    }

    @Override
    public MealOrder orderMeal(Long userId, String ticketId, Long mealId, Integer quantity,
                               String deliveryStation, String instructions) {
        log.info("Ordering meal for user: {}, ticket: {}, meal: {}", userId, ticketId, mealId);

        // Validate quantity
        if (quantity < 1 || quantity > 10) {
            throw new RuntimeException("Quantity must be between 1 and 10");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        // Verify ticket belongs to user
        if (!ticket.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Ticket does not belong to this user");
        }

        if (ticket.getTicketStatus() != TicketStatus.CONFIRMED) {
            throw new RuntimeException("E-pantry is available only for confirmed tickets");
        }

        if (ticket.getJourneyDate() == null || ticket.getJourneyDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("E-pantry can be ordered only for upcoming journeys");
        }

        Meal meal = getMealById(mealId);

        if (!Boolean.TRUE.equals(meal.getAvailabilityStatus()) || !Boolean.TRUE.equals(meal.getIsAvailable())) {
            throw new RuntimeException("Meal is not available");
        }

        // Calculate total price
        double totalPrice = meal.getPrice() * quantity;

        // Create meal order
        MealOrder mealOrder = MealOrder.builder()
                .user(user)
                .ticket(ticket)
                .meal(meal)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .orderDate(LocalDateTime.now())
                .deliveryStatus("PENDING")
                .deliveryStation(deliveryStation)
                .specialInstructions(instructions)
                .build();

        MealOrder savedOrder = mealOrderRepository.save(mealOrder);
        log.info("Meal order placed successfully: {}", savedOrder.getId());
        return savedOrder;
    }

    @Override
    public List<MealOrder> getUserMealOrders(Long userId) {
        log.info("Fetching meal orders for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return mealOrderRepository.findByUserOrderByOrderDateDesc(user);
    }

    @Override
    public MealOrder getMealOrderById(Long orderId) {
        log.info("Fetching meal order by id: {}", orderId);
        return mealOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Meal order not found: " + orderId));
    }

    @Override
    public void cancelMealOrder(Long orderId, Long userId) {
        log.info("Cancelling meal order: {} for user: {}", orderId, userId);

        MealOrder order = getMealOrderById(orderId);

        // Verify ownership
        if (!order.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this meal order");
        }

        // Check if order can be cancelled (only PENDING orders)
        if (!"PENDING".equals(order.getDeliveryStatus())) {
            throw new RuntimeException("Only pending orders can be cancelled");
        }

        order.setDeliveryStatus("CANCELLED");
        order.setRefundStatus("PENDING");
        order.setCancellationDate(LocalDateTime.now());
        mealOrderRepository.save(order);

        log.info("Meal order cancelled successfully: {}", orderId);
    }

    @Override
    public List<MealOrder> getAllMealOrders() {
        log.info("Fetching all meal orders");
        return mealOrderRepository.findAll();
    }

    @Override
    public List<MealOrder> getMealOrdersByStatus(String status) {
        log.info("Fetching meal orders by status: {}", status);
        return mealOrderRepository.findByDeliveryStatus(status);
    }

    @Override
    public List<MealOrder> getMealCancellationRequests() {
        log.info("Fetching meal cancellation requests");
        return mealOrderRepository.findByRefundStatus("PENDING");
    }

    @Override
    public MealOrder approveMealCancellation(Long orderId, Double refundPercentage) {
        log.info("Approving meal cancellation for order: {} with refund: {}%", orderId, refundPercentage);

        MealOrder order = getMealOrderById(orderId);

        Double refundAmount = order.getTotalPrice() * (refundPercentage / 100);

        order.setDeliveryStatus("CANCELLED");
        order.setRefundStatus("APPROVED");
        order.setRefundAmount(refundAmount);
        order.setCancellationDate(LocalDateTime.now());

        MealOrder updatedOrder = mealOrderRepository.save(order);
        log.info("Meal cancellation approved for order: {}, refund amount: ₹{}", orderId, refundAmount);
        return updatedOrder;
    }

    @Override
    public MealOrder rejectMealCancellation(Long orderId, String reason) {
        log.info("Rejecting meal cancellation for order: {}", orderId);

        MealOrder order = getMealOrderById(orderId);

        order.setRefundStatus("REJECTED");
        order.setCancellationReason(reason);

        MealOrder updatedOrder = mealOrderRepository.save(order);
        log.info("Meal cancellation rejected for order: {}", orderId);
        return updatedOrder;
    }

    @Override
    public Double calculateMealRefund(Double totalPrice, LocalDateTime orderDate, LocalDateTime cancellationTime) {
        long hoursUntilDelivery = ChronoUnit.HOURS.between(cancellationTime, orderDate.plusHours(2));

        if (hoursUntilDelivery > 48) {
            return totalPrice * 0.9;
        } else if (hoursUntilDelivery > 24) {
            return totalPrice * 0.7;
        } else if (hoursUntilDelivery > 12) {
            return totalPrice * 0.5;
        } else if (hoursUntilDelivery > 2) {
            return totalPrice * 0.2;
        } else {
            return 0.0;
        }
    }
}
