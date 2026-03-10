package com.railease.controller;

import com.railease.dto.PaymentDTO;
import com.railease.entity.Ticket;
import com.railease.entity.Train;
import com.railease.entity.User;
import com.railease.service.BookingService;
import com.railease.service.PaymentService;
import com.railease.service.PdfService;
import com.railease.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final TrainService trainService;
    private final PdfService pdfService;

    @GetMapping("/process")
    public String showBookingForm(@RequestParam Integer trainNo,
                                  @RequestParam String classType,
                                  @RequestParam(required = false) String journeyDate,
                                  HttpSession session,
                                  Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        Train train = trainService.getTrainByNumber(trainNo);
        LocalDate date = journeyDate != null ? LocalDate.parse(journeyDate) : LocalDate.now();

        int availableSeats = bookingService.getAvailableSeats(trainNo, classType, date);

        model.addAttribute("train", train);
        model.addAttribute("classType", classType);
        model.addAttribute("journeyDate", journeyDate);
        model.addAttribute("availableSeats", availableSeats);
        model.addAttribute("activeUser", user);

        return "user/booking-form";
    }

    @PostMapping("/confirm")
    @Transactional
    public String confirmBooking(@RequestParam Integer trainNo,
                                 @RequestParam String passengerName,
                                 @RequestParam Integer passengerAge,
                                 @RequestParam String passengerGender,
                                 @RequestParam String classType,
                                 @RequestParam Integer numberOfSeats,
                                 @RequestParam String journeyDate,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            LocalDate date = LocalDate.parse(journeyDate);

            int availableSeats = bookingService.getAvailableSeats(trainNo, classType, date);
            if (availableSeats < numberOfSeats) {
                throw new RuntimeException("Only " + availableSeats + " seats available. Please reduce the number of seats.");
            }

            Ticket ticket = bookingService.createTicket(user.getUserId(), trainNo, passengerName,
                    passengerAge, passengerGender, classType, numberOfSeats, date);

            redirectAttributes.addFlashAttribute("ticket", ticket);
            return "redirect:/booking/confirmation/" + ticket.getTicketId();

        } catch (Exception e) {
            log.error("Booking failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/train-details/" + trainNo + "?journeyDate=" + journeyDate;
        }
    }

    @GetMapping("/confirmation/{ticketId}")
    @Transactional(readOnly = true)
    public String showConfirmationPage(@PathVariable String ticketId,
                                  HttpSession session,
                                  Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            if ("PAID".equals(ticket.getPaymentStatus())) {
                return "redirect:/booking/success/" + ticketId;
            }

            Train train = ticket.getTrain();
            String trainName = train.getTrainName();
            String trainNumber = String.valueOf(train.getTrainNo());

            model.addAttribute("ticket", ticket);
            model.addAttribute("trainName", trainName);
            model.addAttribute("trainNumber", trainNumber);
            model.addAttribute("activeUser", user);

            return "user/booking-confirmation";

        } catch (Exception e) {
            log.error("Error showing confirmation page: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

    @GetMapping("/payment/{ticketId}")
    @Transactional(readOnly = true)
    public String showPaymentPage(@PathVariable String ticketId,
                                  HttpSession session,
                                  Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            if ("PAID".equals(ticket.getPaymentStatus())) {
                return "redirect:/booking/success/" + ticketId;
            }

            // Eagerly load train to avoid LazyInitializationException
            Train train = ticket.getTrain();
            String trainName = train.getTrainName();
            String trainNumber = String.valueOf(train.getTrainNo());

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setTicketId(ticketId);
            paymentDTO.setAmount(ticket.getTotalFare());

            model.addAttribute("ticket", ticket);
            model.addAttribute("trainName", trainName);
            model.addAttribute("trainNumber", trainNumber);
            model.addAttribute("activeUser", user);
            model.addAttribute("paymentDTO", paymentDTO);

            return "user/payment";

        } catch (Exception e) {
            log.error("Error showing payment page: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

    @PostMapping("/process-payment")
    @Transactional
    public String processPayment(@ModelAttribute PaymentDTO paymentDTO,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            if (!paymentService.validatePayment(paymentDTO)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid payment details. Please check and try again.");
                return "redirect:/booking/confirmation/" + paymentDTO.getTicketId();
            }

            Ticket ticket = paymentService.processPayment(paymentDTO);

            session.removeAttribute("pendingTicket");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment successful! Your ticket has been confirmed.");

            return "redirect:/booking/success/" + ticket.getTicketId();

        } catch (Exception e) {
            log.error("Payment failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/booking/confirmation/" + paymentDTO.getTicketId();
        }
    }

    @GetMapping("/success/{ticketId}")
    @Transactional(readOnly = true)
    public String bookingSuccess(@PathVariable String ticketId,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            // Eagerly load train to avoid LazyInitializationException
            Train train = ticket.getTrain();
            String trainName = train.getTrainName();
            String trainNumber = String.valueOf(train.getTrainNo());

            model.addAttribute("ticket", ticket);
            model.addAttribute("trainName", trainName);
            model.addAttribute("trainNumber", trainNumber);
            model.addAttribute("activeUser", user);

            return "user/booking-success";

        } catch (Exception e) {
            log.error("Error showing success page: {}", e.getMessage());
            return "redirect:/user/dashboard";
        }
    }

    @GetMapping("/download-pdf/{ticketId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable String ticketId,
                                                     HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            // Eagerly load train to avoid LazyInitializationException
            ticket.getTrain().getTrainName();

            ByteArrayOutputStream pdfStream = pdfService.generateTicketPdf(ticket);
            byte[] pdfBytes = pdfStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "RailEase-Ticket-" + ticketId + ".pdf");

            log.info("PDF downloaded for ticket: {}", ticketId);
            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generating PDF for ticket: {}", ticketId, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/view/{ticketId}")
    @Transactional(readOnly = true)
    public String viewTicket(@PathVariable String ticketId,
                             HttpSession session,
                             Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            // Eagerly load train to avoid LazyInitializationException
            Train train = ticket.getTrain();
            String trainName = train.getTrainName();
            String trainNumber = String.valueOf(train.getTrainNo());

            model.addAttribute("ticket", ticket);
            model.addAttribute("trainName", trainName);
            model.addAttribute("trainNumber", trainNumber);
            model.addAttribute("activeUser", user);

            return "user/ticket-details";

        } catch (Exception e) {
            log.error("Error viewing ticket: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }
}