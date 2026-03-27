package com.railease.controller;

import com.railease.constants.UserRole;
import com.railease.dto.CancellationRuleDTO;
import com.railease.dto.MealDTO;
import com.railease.dto.TrainDTO;
import com.railease.entity.Meal;
import com.railease.entity.MealOrder;
import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
import com.railease.service.CancellationService;
import com.railease.service.MealService;
import com.railease.service.TrainService;
import com.railease.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final TrainService trainService;
    private final MealService mealService;
    private final UserService userService;
    private final CancellationService cancellationService;

    // Helper method to check admin access
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        return user != null && UserRole.ADMIN.equals(user.getRole());
    }

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        User admin = (User) session.getAttribute("activeUser");
        model.addAttribute("activeUser", admin);

        // Statistics for dashboard
        model.addAttribute("totalTrains", trainService.getAllTrains().size());
        model.addAttribute("activeTrains", trainService.getAllActiveTrains().size());
        model.addAttribute("totalMeals", mealService.getAllMeals().size());
        model.addAttribute("availableMeals", mealService.getAvailableMeals().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());

        // Cancellation stats
        model.addAttribute("cancellationStats", cancellationService.getCancellationStatistics());

        // Recent cancellation requests
        model.addAttribute("recentCancellations",
                cancellationService.getAllCancellationRequests());

        return "admin/dashboard";
    }

    // ==================== TRAIN MANAGEMENT ====================

    @GetMapping("/trains")
    public String manageTrains(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Train> trains = trainService.getAllTrains();
        model.addAttribute("trains", trains);
        model.addAttribute("activeUser", session.getAttribute("activeUser"));

        return "admin/manage-trains";
    }

    @GetMapping("/trains/add")
    public String showAddTrainForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("trainDTO", new TrainDTO());
        model.addAttribute("activeUser", session.getAttribute("activeUser"));

        return "admin/add-train";
    }



    @GetMapping("/trains/edit/{id}")
    public String showEditTrainForm(@PathVariable Integer id,
                                    HttpSession session,
                                    Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Train train = trainService.getTrainById(id);

            // Convert to DTO with seat and fare fields
            TrainDTO trainDTO = new TrainDTO();
            trainDTO.setId(train.getTrainNo());
            trainDTO.setTrainName(train.getTrainName());
            trainDTO.setSource(train.getSource() != null ? train.getSource() : train.getSourceStation());
            trainDTO.setDestination(train.getDestination() != null ? train.getDestination() : train.getDestinationStation());
            trainDTO.setDepartureTime(train.getDepartureTime());
            trainDTO.setArrivalTime(train.getArrivalTime());
            trainDTO.setTravelDate(train.getTravelDate() != null ? train.getTravelDate() : train.getJourneyDate());
            trainDTO.setAvailableSeats(train.getAvailableSeats());
            // Seat fields
            trainDTO.setAcSeats(train.getAcSeats());
            trainDTO.setSleeperSeats(train.getSleeperSeats());
            trainDTO.setGeneralSeats(train.getGeneralSeats());
            // Fare fields
            trainDTO.setAcFare(train.getAcFare());
            trainDTO.setSleeperFare(train.getSleeperFare());
            trainDTO.setGeneralFare(train.getGeneralFare());
            trainDTO.setIsActive(train.getIsActive());

            model.addAttribute("trainDTO", trainDTO);
            model.addAttribute("activeUser", session.getAttribute("activeUser"));

            return "admin/edit-train";
        } catch (Exception e) {
            log.error("Error loading train for edit: {}", e.getMessage());
            return "redirect:/admin/trains";
        }
    }


    // ==================== USER MANAGEMENT ====================

    @GetMapping("/users")
    public String viewUsers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("activeUser", session.getAttribute("activeUser"));

        return "admin/view-users";
    }

    @GetMapping("/users/toggle/{userId}")
    public String toggleUserStatus(@PathVariable Long userId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            User user = userService.findById(userId);
            user.setIsEnabled(!user.getIsEnabled());
            userService.updateProfile(user);

            String status = user.getIsEnabled() ? "enabled" : "disabled";
            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + user.getUsername() + " " + status + " successfully!");
        } catch (Exception e) {
            log.error("Error toggling user status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // ==================== MEAL MANAGEMENT ====================

    @GetMapping("/meals")
    public String manageMeals(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Meal> meals = mealService.getAllMeals();
        model.addAttribute("meals", meals);
        model.addAttribute("activeUser", session.getAttribute("activeUser"));

        return "admin/manage-meals";
    }

    @GetMapping("/meals/add")
    public String showAddMealForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("mealDTO", new MealDTO());
        model.addAttribute("activeUser", session.getAttribute("activeUser"));

        return "admin/add-meal";
    }

    @PostMapping("/meals/add")
    public String addMeal(@Valid @ModelAttribute("mealDTO") MealDTO mealDTO,
                          BindingResult result,
                          HttpSession session,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("activeUser", session.getAttribute("activeUser"));
            return "admin/add-meal";
        }

        try {
            mealService.createMeal(mealDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal " + mealDTO.getMealName() + " added successfully!");
        } catch (Exception e) {
            log.error("Error adding meal: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/meals";
    }

    @GetMapping("/meals/edit/{id}")
    public String showEditMealForm(@PathVariable Long id,
                                   HttpSession session,
                                   Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Meal meal = mealService.getMealById(id);

            // Convert to DTO
            MealDTO mealDTO = new MealDTO();
            mealDTO.setId(meal.getId());
            mealDTO.setMealName(meal.getMealName());
            mealDTO.setDescription(meal.getDescription());
            mealDTO.setPrice(meal.getPrice());
            mealDTO.setAvailabilityStatus(meal.getAvailabilityStatus());
            mealDTO.setMealType(meal.getMealType());
            mealDTO.setPreparationTime(meal.getPreparationTime());

            model.addAttribute("mealDTO", mealDTO);
            model.addAttribute("activeUser", session.getAttribute("activeUser"));

            return "admin/update-meal";
        } catch (Exception e) {
            log.error("Error loading meal for edit: {}", e.getMessage());
            return "redirect:/admin/meals";
        }
    }

    @PostMapping("/meals/update/{id}")
    public String updateMeal(@PathVariable Long id,
                             @Valid @ModelAttribute("mealDTO") MealDTO mealDTO,
                             BindingResult result,
                             HttpSession session,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("activeUser", session.getAttribute("activeUser"));
            return "admin/update-meal";
        }

        try {
            mealService.updateMeal(id, mealDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal updated successfully!");
        } catch (Exception e) {
            log.error("Error updating meal: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/meals";
    }

    @GetMapping("/meals/delete/{id}")
    public String deleteMeal(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            mealService.deleteMeal(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting meal: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/meals";
    }

    @GetMapping("/meals/toggle/{id}")
    public String toggleMealAvailability(@PathVariable Long id,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Meal meal = mealService.toggleMealAvailability(id);
            String status = meal.getAvailabilityStatus() ? "available" : "unavailable";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal marked as " + status + " successfully!");
        } catch (Exception e) {
            log.error("Error toggling meal availability: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/meals";
    }

    // ==================== CANCELLATION MANAGEMENT ====================

    @GetMapping("/cancellations")
    public String viewCancellationRequests(HttpSession session, Model model) {
        return viewCancellationRequests(null, null, null, session, model);
    }

    @GetMapping("/cancellations/filter")
    public String viewCancellationRequests(@RequestParam(required = false) String refundStatus,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) String ticketId,
                                           HttpSession session,
                                           Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("ticketRequests", cancellationService.getTicketCancellationRequests());
        model.addAttribute("mealRequests", cancellationService.getMealCancellationRequests());
        model.addAttribute("stats", cancellationService.getCancellationStatistics());
        List<Ticket> history = cancellationService.getTicketCancellationHistory(refundStatus, userId, ticketId);
        model.addAttribute("history", history);
        model.addAttribute("processingHistory",
                cancellationService.getTicketCancellationHistory("PROCESSING", null, null));
        model.addAttribute("rules", cancellationService.getCancellationRules());
        model.addAttribute("ruleForm", new CancellationRuleDTO());
        model.addAttribute("selectedRefundStatus", refundStatus);
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedTicketId", ticketId);
        model.addAttribute("activeUser", session.getAttribute("activeUser"));

        return "admin/cancellation-requests";
    }

    @PostMapping("/cancellations/ticket/approve")
    public String approveTicketCancellation(@RequestParam String ticketId,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            cancellationService.approveTicketCancellation(ticketId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ticket cancellation approved. Refund moved to processing.");
        } catch (Exception e) {
            log.error("Error approving ticket cancellation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/cancellations";
    }

    @PostMapping("/cancellations/ticket/reject")
    public String rejectTicketCancellation(@RequestParam String ticketId,
                                           @RequestParam String reason,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            cancellationService.rejectTicketCancellation(ticketId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ticket cancellation rejected!");
        } catch (Exception e) {
            log.error("Error rejecting ticket cancellation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/cancellations";
    }

    @PostMapping("/cancellations/ticket/complete")
    public String completeTicketRefund(@RequestParam String ticketId,
                                       @RequestParam(required = false) String transactionId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            cancellationService.completeTicketRefund(ticketId, transactionId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Refund marked as completed and user notification sent.");
        } catch (Exception e) {
            log.error("Error completing refund: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/cancellations";
    }

    @PostMapping("/cancellations/meal/approve")
    public String approveMealCancellation(@RequestParam Long orderId,
                                          @RequestParam Double refundPercentage,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            cancellationService.approveMealCancellation(orderId, refundPercentage);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal cancellation approved successfully!");
        } catch (Exception e) {
            log.error("Error approving meal cancellation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/cancellations";
    }

    @PostMapping("/cancellations/meal/reject")
    public String rejectMealCancellation(@RequestParam Long orderId,
                                         @RequestParam String reason,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            cancellationService.rejectMealCancellation(orderId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meal cancellation rejected!");
        } catch (Exception e) {
            log.error("Error rejecting meal cancellation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/cancellations";
    }

    @GetMapping("/cancellations/calculate-refund")
    @ResponseBody
    public Map<String, Object> calculateRefund(@RequestParam java.time.LocalDateTime departureTime,
                                               @RequestParam java.time.LocalDateTime cancellationTime) {
        Map<String, Object> result = new HashMap<>();
        Double percentage = cancellationService.calculateRefundPercentage(departureTime, cancellationTime);
        result.put("refundPercentage", percentage);
        return result;
    }

    @PostMapping("/cancellations/rules")
    public String saveCancellationRule(@Valid @ModelAttribute("ruleForm") CancellationRuleDTO ruleForm,
                                       BindingResult result,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid cancellation rule details.");
            return "redirect:/admin/cancellations";
        }

        try {
            cancellationService.saveCancellationRule(ruleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cancellation rule saved successfully.");
        } catch (Exception e) {
            log.error("Error saving cancellation rule: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/cancellations";
    }

    @PostMapping("/cancellations/rules/delete/{ruleId}")
    public String deleteCancellationRule(@PathVariable Long ruleId,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            cancellationService.deleteCancellationRule(ruleId);
            redirectAttributes.addFlashAttribute("successMessage", "Cancellation rule deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting cancellation rule: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/cancellations";
    }

    @PostMapping("/trains/add")
    public String addTrain(@Valid @ModelAttribute("trainDTO") TrainDTO trainDTO,
                           BindingResult result,
                           HttpSession session,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("activeUser", session.getAttribute("activeUser"));
            return "admin/add-train";
        }

        try {
            trainService.createTrain(trainDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Train " + trainDTO.getTrainName() + " added successfully!");
        } catch (Exception e) {
            log.error("Error adding train: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/trains";
    }

    @PostMapping("/trains/update/{id}")
    public String updateTrain(@PathVariable Integer id,
                              @ModelAttribute TrainDTO trainDTO,
                              BindingResult result,
                              HttpSession session,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Calculate total available seats from individual seat categories
        if (trainDTO.getAcSeats() != null && trainDTO.getSleeperSeats() != null && trainDTO.getGeneralSeats() != null) {
            int totalSeats = trainDTO.getAcSeats() + trainDTO.getSleeperSeats() + trainDTO.getGeneralSeats();
            trainDTO.setAvailableSeats(totalSeats);
            log.info("Calculated total seats: {}", totalSeats);
        }
        
        log.info("Train DTO received - acSeats: {}, sleeperSeats: {}, generalSeats: {}, acFare: {}, sleeperFare: {}, generalFare: {}",
                trainDTO.getAcSeats(), trainDTO.getSleeperSeats(), trainDTO.getGeneralSeats(),
                trainDTO.getAcFare(), trainDTO.getSleeperFare(), trainDTO.getGeneralFare());

        try {
            trainService.updateTrain(id, trainDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Train updated successfully!");
        } catch (Exception e) {
            log.error("Error updating train: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/trains";
    }

    @GetMapping("/trains/delete/{id}")
    public String deleteTrain(@PathVariable Integer id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            trainService.deleteTrain(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Train deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting train: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/trains";
    }

    @GetMapping("/trains/toggle/{id}")
    public String toggleTrainStatus(@PathVariable Integer id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Train train = trainService.toggleTrainStatus(id);
            String status = train.getIsActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Train " + status + " successfully!");
        } catch (Exception e) {
            log.error("Error toggling train status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/trains";
    }
}
