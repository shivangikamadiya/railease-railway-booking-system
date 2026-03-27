package com.railease.controller;

import com.railease.dto.CancellationRequestDTO;
import com.railease.dto.CancellationResponseDTO;
import com.railease.dto.RefundStatusDTO;
import com.railease.entity.User;
import com.railease.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/cancellation")
@RequiredArgsConstructor
@Slf4j
public class CancellationController {

    private final BookingService bookingService;

    @PostMapping("/confirm")
    public String confirmCancellation(@ModelAttribute CancellationRequestDTO request,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            CancellationResponseDTO response = bookingService.processCancellation(
                    request.getTicketId(),
                    user.getUserId(),
                    request.getReason()
            );
            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
            return "redirect:/cancellation/status/" + request.getTicketId();
        } catch (Exception e) {
            log.error("Cancellation failed for {}: {}", request.getTicketId(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/cancel-ticket/" + request.getTicketId();
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
            RefundStatusDTO refundStatus = bookingService.checkRefundStatus(ticketId, user.getUserId());
            model.addAttribute("refundStatus", refundStatus);
            model.addAttribute("activeUser", user);
            return "user/refund-status";
        } catch (Exception e) {
            log.error("Error fetching refund status for {}: {}", ticketId, e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }
}
