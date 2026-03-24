package com.railease.repository;

import com.railease.entity.MealOrder;
import com.railease.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealOrderRepository extends JpaRepository<MealOrder, Long> {

    @Query("SELECT mo FROM MealOrder mo " +
            "LEFT JOIN FETCH mo.meal " +
            "LEFT JOIN FETCH mo.ticket t " +
            "LEFT JOIN FETCH t.train " +
            "WHERE mo.user = :user " +
            "ORDER BY mo.orderDate DESC")
    List<MealOrder> findByUserOrderByOrderDateDesc(@Param("user") User user);

    @Query("SELECT mo FROM MealOrder mo WHERE mo.deliveryStatus = :status")
    List<MealOrder> findByDeliveryStatus(@Param("status") String status);

    @Query("SELECT mo FROM MealOrder mo WHERE mo.refundStatus = :refundStatus")
    List<MealOrder> findByRefundStatus(@Param("refundStatus") String refundStatus);

    @Query("SELECT mo FROM MealOrder mo WHERE mo.cancellationDate IS NOT NULL")
    List<MealOrder> findCancellationRequests();

    @Query("SELECT mo FROM MealOrder mo WHERE mo.orderDate BETWEEN :startDate AND :endDate")
    List<MealOrder> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(mo) FROM MealOrder mo WHERE mo.meal.id = :mealId")
    Long countOrdersByMealId(@Param("mealId") Long mealId);

    @Query("SELECT COALESCE(SUM(mo.totalPrice), 0) FROM MealOrder mo WHERE mo.deliveryStatus = 'DELIVERED'")
    Double getTotalRevenue();
}
