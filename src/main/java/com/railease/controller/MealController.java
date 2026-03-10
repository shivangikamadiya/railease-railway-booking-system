package com.railease.controller;

import com.railease.entity.Meal;
import com.railease.entity.MealOrder;
import com.railease.entity.Ticket;
import com.railease.entity.User;
import com.railease.service.BookingService;
import com.railease.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/meal")
@RequiredArgsConstructor
@Slf4j
public class MealController {

    private final MealService mealService;
    private final BookingService bookingService;

    @GetMapping("/menu")
    public String showMealMenu(Model model, HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<Meal> meals = mealService.getAvailableMeals();
        List<Ticket> activeTickets = bookingService.getUserActiveBookings(user.getUserId());

        model.addAttribute("meals", meals);
        model.addAttribute("activeTickets", activeTickets);
        model.addAttribute("activeUser", user);

        return "user/meal-menu";
    }

    @GetMapping("/order/{ticketId}")
    public String showOrderForm(@PathVariable String ticketId,
                                HttpSession session,
                                Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            // Verify ticket belongs to user
            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            // Get meals for this train
            List<Meal> meals = mealService.getMealsByTrain(ticket.getTrain().getTrainNo());

            // Get stations list for delivery
            List<String> stations = List.of(
                    ticket.getTrain().getSource(),
                    "Station 1", "Station 2", "Station 3",
                    ticket.getTrain().getDestination()
            );

            model.addAttribute("ticket", ticket);
            model.addAttribute("meals", meals);
            model.addAttribute("stations", stations);
            model.addAttribute("activeUser", user);

            return "user/meal-order";
        } catch (Exception e) {
            log.error("Error showing meal order form: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

    @PostMapping("/place-order")
    public String placeOrder(@RequestParam String ticketId,
                             @RequestParam Long mealId,
                             @RequestParam Integer quantity,
                             @RequestParam String deliveryStation,
                             @RequestParam(required = false) String instructions,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            MealOrder order = mealService.orderMeal(user.getUserId(), ticketId, mealId,
                    quantity, deliveryStation, instructions);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal ordered successfully! Order ID: " + order.getId());
        } catch (Exception e) {
            log.error("Meal order failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/my-meal-orders";
    }

    @GetMapping("/my-orders")
    public String myOrders(HttpSession session, Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<MealOrder> orders = mealService.getUserMealOrders(user.getUserId());
        model.addAttribute("orders", orders);
        model.addAttribute("activeUser", user);

        return "user/my-meal-orders";
    }

    @GetMapping("/order-details/{orderId}")
    public String viewOrderDetails(@PathVariable Long orderId,
                                   HttpSession session,
                                   Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            MealOrder order = mealService.getMealOrderById(orderId);

            if (!order.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-meal-orders";
            }

            model.addAttribute("order", order);
            model.addAttribute("activeUser", user);

            return "user/meal-order-details";
        } catch (Exception e) {
            log.error("Error viewing meal order: {}", e.getMessage());
            return "redirect:/user/my-meal-orders";
        }
    }

    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            mealService.cancelMealOrder(orderId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Meal order cancelled successfully");
        } catch (Exception e) {
            log.error("Meal order cancellation failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/my-meal-orders";
    }
}