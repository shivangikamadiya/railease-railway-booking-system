package com.railease.controller;

import com.railease.dto.RefundEstimateDTO;
import com.railease.entity.Meal;
import com.railease.entity.MealOrder;
import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
import com.railease.service.BookingService;
import com.railease.service.MealService;
import com.railease.service.TrainService;
import com.railease.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final TrainService trainService;
    private final BookingService bookingService;
    private final UserService userService;
    private final MealService mealService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Basic user info
            model.addAttribute("activeUser", user);

            // Statistics
            Long totalBookings = bookingService.getUserBookingCount(user.getUserId());
            Long upcomingJourneys = bookingService.getUserUpcomingJourneysCount(user.getUserId());

            // Calculate total spent using the new method
            Double totalSpent = bookingService.getTotalSpentByUser(user.getUserId());

            // Recent bookings (last 5)
            List<Ticket> recentBookings = bookingService.getUserRecentBookings(user.getUserId(), 5);

            // Active bookings for meal ordering
            List<Ticket> activeTickets = bookingService.getUserActiveBookings(user.getUserId());

            // Booking counts
            int activeBookings = activeTickets.size();
            int pastBookings = bookingService.getUserPastBookings(user.getUserId()).size();

            // Meal orders
            int totalMealOrders = mealService.getUserMealOrders(user.getUserId()).size();

            // Featured meals for E-Pantry (top 4)
            List<Meal> featuredMeals = mealService.getAvailableMeals();
            if (featuredMeals.size() > 4) {
                featuredMeals = featuredMeals.subList(0, 4);
            }

            // Refund stats
            Map<String, Long> refundStats = bookingService.getUserRefundStats(user.getUserId());
            Long pendingRefunds = refundStats.getOrDefault("pendingRefunds", 0L);

            // Add all to model
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("upcomingJourneys", upcomingJourneys);
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("recentBookings", recentBookings);
            model.addAttribute("activeTickets", activeTickets);
            model.addAttribute("activeBookings", activeBookings);
            model.addAttribute("pastBookings", pastBookings);
            model.addAttribute("totalMealOrders", totalMealOrders);
            model.addAttribute("featuredMeals", featuredMeals);
            model.addAttribute("pendingRefunds", pendingRefunds);

            return "user/dashboard";

        } catch (Exception e) {
            log.error("Error loading dashboard: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading dashboard");
            return "error";
        }
    }

    @GetMapping("/view-trains")
    public String viewTrains(Model model, HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            List<Train> trains = trainService.getAllActiveTrains();
            model.addAttribute("trains", trains);
            model.addAttribute("activeUser", user);
            model.addAttribute("today", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

            log.info("Loaded {} active trains", trains.size());
        } catch (Exception e) {
            log.error("Error loading trains: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading trains");
        }

        return "user/view-trains";
    }

    @GetMapping("/search-trains")
    public String searchTrains(@RequestParam(required = false) String source,
                               @RequestParam(required = false) String destination,
                               @RequestParam(required = false) String journeyDate,
                               Model model, HttpSession session) {

        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("activeUser", user);

        try {
            if (source != null && destination != null && journeyDate != null
                    && !source.trim().isEmpty() && !destination.trim().isEmpty()) {

                LocalDate date = LocalDate.parse(journeyDate);
                log.info("Searching trains from {} to {} on {}", source, destination, date);

                List<Train> trains = trainService.findTrainsBetweenStations(source, destination, date);

                model.addAttribute("trains", trains);
                model.addAttribute("source", source);
                model.addAttribute("destination", destination);
                model.addAttribute("journeyDate", journeyDate);

                if (trains.isEmpty()) {
                    model.addAttribute("infoMessage", "No trains found for this route");
                }
            }
        } catch (Exception e) {
            log.error("Search error: {}", e.getMessage());
            model.addAttribute("errorMessage", "Invalid search parameters");
        }

        return "user/view-trains";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model, HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            List<Ticket> allBookings = bookingService.getUserBookings(user.getUserId());
            List<Ticket> activeBookings = bookingService.getUserActiveBookings(user.getUserId());
            List<Ticket> pastBookings = bookingService.getUserPastBookings(user.getUserId());
            List<Ticket> cancelledBookings = bookingService.getUserCancelledBookings(user.getUserId());

            model.addAttribute("allBookings", allBookings);
            model.addAttribute("activeBookings", activeBookings);
            model.addAttribute("pastBookings", pastBookings);
            model.addAttribute("cancelledBookings", cancelledBookings);
            model.addAttribute("activeUser", user);

            log.info("Loaded {} total bookings for user", allBookings.size());

        } catch (Exception e) {
            log.error("Error loading bookings: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading bookings");
        }

        return "user/my-bookings";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            user = userService.findById(user.getUserId());
            session.setAttribute("activeUser", user);

            Long totalBookings = bookingService.getUserBookingCount(user.getUserId());
            Long upcomingBookings = bookingService.getUserUpcomingJourneysCount(user.getUserId());
            Double totalSpent = bookingService.getTotalSpentByUser(user.getUserId());

            model.addAttribute("user", user);
            model.addAttribute("activeUser", user);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("upcomingBookings", upcomingBookings);
            model.addAttribute("totalSpent", totalSpent != null ? totalSpent : 0.0);

        } catch (Exception e) {
            log.error("Error loading profile: {}", e.getMessage());
        }

        return "user/profile";
    }

    @GetMapping("/edit-profile")
    public String editProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            user = userService.findById(user.getUserId());
            model.addAttribute("user", user);
            model.addAttribute("activeUser", user);
        } catch (Exception e) {
            log.error("Error loading edit profile: {}", e.getMessage());
        }

        return "user/edit-profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String mobileNumber,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            user.setFullName(fullName.trim());
            user.setMobileNumber(mobileNumber);

            User updatedUser = userService.updateProfile(user);
            session.setAttribute("activeUser", updatedUser);

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
        } catch (Exception e) {
            log.error("Profile update failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("activeUser", user);
        return "user/change-password";
    }

    @PostMapping("/update-password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match");
                return "redirect:/user/change-password";
            }

            userService.changePassword(user.getUserId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully");
        } catch (Exception e) {
            log.error("Password change failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/profile";
    }

    @GetMapping("/meal-menu")
    public String mealMenu(Model model, HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            List<Meal> meals = mealService.getAvailableMeals();
            List<Ticket> activeTickets = bookingService.getUserActiveBookings(user.getUserId());

            model.addAttribute("meals", meals);
            model.addAttribute("activeTickets", activeTickets);
            model.addAttribute("activeUser", user);

            log.info("Loaded {} meals for menu", meals.size());

        } catch (Exception e) {
            log.error("Error loading meal menu: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading meal menu");
        }

        return "user/meal-menu";
    }

    @GetMapping("/meal-order/{ticketId}")
    public String mealOrderForm(@PathVariable String ticketId,
                                HttpSession session,
                                Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            List<Meal> meals = mealService.getMealsByTrain(ticket.getTrain().getTrainNo());

            // Get stations list
            List<String> stations = List.of(
                    ticket.getTrain().getSource(),
                    "Station 1", "Station 2", "Station 3",
                    ticket.getTrain().getDestination()
            );

            model.addAttribute("ticket", ticket);
            model.addAttribute("meals", meals);
            model.addAttribute("stations", stations);
            model.addAttribute("activeUser", user);

        } catch (Exception e) {
            log.error("Error loading meal order form: {}", e.getMessage());
            return "redirect:/user/meal-menu";
        }

        return "user/meal-order";
    }

    @GetMapping("/my-meal-orders")
    public String myMealOrders(Model model, HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            List<MealOrder> orders = mealService.getUserMealOrders(user.getUserId());
            model.addAttribute("orders", orders);
            model.addAttribute("activeUser", user);

            log.info("Loaded {} meal orders for user", orders.size());

        } catch (Exception e) {
            log.error("Error loading meal orders: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error loading meal orders");
        }

        return "user/my-meal-orders";
    }

    @GetMapping("/cancel-ticket/{ticketId}")
    public String showCancellationPage(@PathVariable String ticketId,
                                       HttpSession session,
                                       Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            RefundEstimateDTO estimate = bookingService.calculateRefundEstimate(ticketId, user.getUserId());
            model.addAttribute("refundEstimate", estimate);
            model.addAttribute("activeUser", user);

            return "user/cancel-ticket";

        } catch (Exception e) {
            log.error("Error showing cancellation page: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

    @GetMapping("/ticket/{ticketId}")
    public String viewTicket(@PathVariable String ticketId,
                             HttpSession session,
                             Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            model.addAttribute("ticket", ticket);
            model.addAttribute("activeUser", user);

            return "user/ticket-details";

        } catch (Exception e) {
            log.error("Error viewing ticket: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

    @GetMapping("/book/{trainNo}")
    public String bookTicket(@PathVariable Integer trainNo,
                             @RequestParam String classType,
                             @RequestParam(required = false) String journeyDate,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("activeUser");
        if (user == null) return "redirect:/login";

        try {
            LocalDate date = journeyDate != null ? LocalDate.parse(journeyDate) : LocalDate.now();

            // Redirect to booking controller
            return "redirect:/booking/process?trainNo=" + trainNo +
                    "&classType=" + classType +
                    "&journeyDate=" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        } catch (Exception e) {
            log.error("Error redirecting to booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/train-details/" + trainNo;
        }
    }

    @GetMapping("/train-details/{trainNo}")
    public String trainDetails(@PathVariable Integer trainNo,
                               @RequestParam(required = false) String journeyDate,
                               Model model,
                               HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        Train train = trainService.getTrainByNumber(trainNo);

        // Parse journey date or use default
        LocalDate date = journeyDate != null ? LocalDate.parse(journeyDate) : LocalDate.now();

        // Get available seats using the new method
        int availableSeats = bookingService.getAvailableSeats(trainNo, "GENERAL", date);

        model.addAttribute("train", train);
        model.addAttribute("availableSeats", availableSeats);
        model.addAttribute("ticketPrice", train.getTicketPrice());
        model.addAttribute("source", train.getSource());
        model.addAttribute("destination", train.getDestination());
        model.addAttribute("selectedDate", journeyDate != null ? journeyDate : LocalDate.now().toString());
        model.addAttribute("activeUser", user);

        return "user/train-details";
    }
}