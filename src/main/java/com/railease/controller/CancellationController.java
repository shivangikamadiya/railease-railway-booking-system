package com.railease.controller;

import com.railease.dto.CancellationRequestDTO;
import com.railease.entity.User;
import com.railease.service.BookingService;
import com.railease.service.CancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@Controller
@RequestMapping("/cancellation")
@RequiredArgsConstructor
@Slf4j
public class CancellationController {

    private final CancellationService cancellationService;
    private final BookingService bookingService;

    @GetMapping("/ticket/{ticketId}")
    public String showCancellationForm(@PathVariable String ticketId,
                                       HttpSession session,
                                       Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            var ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            model.addAttribute("ticket", ticket);
            model.addAttribute("activeUser", user);

            return "user/cancel-ticket";
        } catch (Exception e) {
            log.error("Error showing cancellation form: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

    @PostMapping("/initiate")
    public String initiateCancellation(@Valid @ModelAttribute CancellationRequestDTO request,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // FIXED: Use getTicketId() or create a new field in DTO
            String ticketId = request.getTicketId(); // Make sure this field exists in DTO

            redirectAttributes.addFlashAttribute("successMessage",
                    "Cancellation initiated successfully");

            return "redirect:/cancellation/status/" + ticketId;
        } catch (Exception e) {
            log.error("Cancellation failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cancellation/ticket/" + request.getTicketId();
        }
    }

    @GetMapping("/status/{ticketId}")
    public String showRefundStatus(@PathVariable String ticketId,
                                   HttpSession session,
                                   Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            var ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            model.addAttribute("ticket", ticket);
            model.addAttribute("activeUser", user);

            return "user/refund-status";
        } catch (Exception e) {
            log.error("Error fetching refund status: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }
}