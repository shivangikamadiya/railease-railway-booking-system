package com.railease.service.impl;

import com.railease.constants.TicketStatus;
import com.railease.dto.PaymentDTO;
import com.railease.entity.Ticket;
import com.railease.repository.TicketRepository;
import com.railease.service.BookingService;
import com.railease.service.EmailService;
import com.railease.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TicketRepository ticketRepository;
    private final BookingService bookingService;
    private final EmailService emailService;
    private final Random random = new Random();

    @Override
    public Ticket processPayment(PaymentDTO paymentDTO) {
        log.info("Processing payment for ticket: {}", paymentDTO.getTicketId());

        // Get the ticket
        Ticket ticket = ticketRepository.findById(paymentDTO.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + paymentDTO.getTicketId()));

        // Check if ticket is already paid
        if ("PAID".equals(ticket.getPaymentStatus())) {
            throw new RuntimeException("Ticket is already paid for");
        }

        // Validate payment details based on method
        if (!validatePayment(paymentDTO)) {
            throw new RuntimeException("Payment validation failed. Please check your payment details.");
        }

        // Simulate payment gateway processing with realistic behavior
        boolean paymentSuccess = simulatePaymentGateway(paymentDTO);

        if (!paymentSuccess) {
            throw new RuntimeException("Payment failed. Please try again with different payment method.");
        }

        // Generate payment ID
        String paymentId = generatePaymentId(paymentDTO.getPaymentMethod());

        // Update ticket with payment details
        ticket.setPaymentId(paymentId);
        ticket.setPaymentMethod(paymentDTO.getPaymentMethod());
        ticket.setPaymentStatus("PAID");
        ticket.setBookingStatus("CONFIRMED");
        ticket.setTicketStatus(TicketStatus.CONFIRMED);

        // Save the updated ticket
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Payment processed successfully. Payment ID: {}", paymentId);

        // Send confirmation email asynchronously
        try {
            emailService.sendBookingConfirmationEmail(
                    savedTicket.getUser(),
                    savedTicket.getTicketId(),
                    savedTicket.getTrain().getTrainName(),
                    savedTicket.getJourneyDate().toString(),
                    savedTicket.getTotalFare()
            );
        } catch (Exception e) {
            log.error("Failed to send confirmation email: {}", e.getMessage());
            // Don't throw exception - payment is already successful
        }

        return savedTicket;
    }

    private boolean simulatePaymentGateway(PaymentDTO paymentDTO) {
        // Simulate network delay
        try {
            Thread.sleep(1500); // 1.5 second delay to simulate real payment
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Different success rates based on payment method for realism
        double successRate = switch (paymentDTO.getPaymentMethod()) {
            case "CARD" -> 0.95; // 95% success rate for cards
            case "UPI" -> 0.92;   // 92% success rate for UPI
            case "NETBANKING" -> 0.90; // 90% success rate for netbanking
            default -> 0.85;
        };

        // Simulate random failures (but make it deterministic for testing)
        // For testing, use specific test card numbers that always work
        if (paymentDTO.getCardNumber() != null) {
            // Test card numbers that always succeed
            if (paymentDTO.getCardNumber().replaceAll("\\s", "").equals("4111111111111111") ||
                    paymentDTO.getCardNumber().replaceAll("\\s", "").equals("5555555555554444")) {
                return true;
            }

            // Test card numbers that always fail
            if (paymentDTO.getCardNumber().replaceAll("\\s", "").equals("4000000000000002")) {
                return false;
            }
        }

        // Random success based on rate
        return random.nextDouble() < successRate;
    }

    private String generatePaymentId(String paymentMethod) {
        String prefix = switch (paymentMethod) {
            case "CARD" -> "CARD";
            case "UPI" -> "UPI";
            case "NETBANKING" -> "NB";
            default -> "PAY";
        };

        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return prefix + timestamp + randomPart;
    }

    @Override
    public boolean validatePayment(PaymentDTO paymentDTO) {
        log.debug("Validating payment for method: {}", paymentDTO.getPaymentMethod());

        // Basic validation
        if (paymentDTO.getAmount() <= 0) {
            log.warn("Invalid amount: {}", paymentDTO.getAmount());
            return false;
        }

        // Payment method specific validation
        return switch (paymentDTO.getPaymentMethod()) {
            case "CARD" -> validateCardPayment(paymentDTO);
            case "UPI" -> validateUPIPayment(paymentDTO);
            case "NETBANKING" -> validateNetBankingPayment(paymentDTO);
            default -> {
                log.warn("Unknown payment method: {}", paymentDTO.getPaymentMethod());
                yield false;
            }
        };
    }

    private boolean validateCardPayment(PaymentDTO paymentDTO) {
        if (paymentDTO.getCardNumber() == null || paymentDTO.getCardNumber().trim().isEmpty()) {
            log.warn("Card number is missing");
            return false;
        }

        // Remove spaces and dashes
        String cardNumber = paymentDTO.getCardNumber().replaceAll("[\\s-]", "");

        // Check length (13-19 digits for most cards)
        if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            log.warn("Invalid card number length: {}", cardNumber.length());
            return false;
        }

        // Check if all characters are digits
        if (!cardNumber.matches("\\d+")) {
            log.warn("Card number contains non-digit characters");
            return false;
        }

        // Check for test card numbers FIRST - bypass Luhn check for known test cards
        if (cardNumber.equals("4111111111111111") || 
            cardNumber.equals("5555555555554444") ||
            cardNumber.equals("4000000000000002")) {
            log.info("Test card detected, bypassing Luhn validation");
            // For test cards, just validate basic fields
            return validateBasicCardFields(paymentDTO);
        }

        // Luhn algorithm check (basic validation) - only for non-test cards
        if (!luhnCheck(cardNumber)) {
            log.warn("Card number failed Luhn check");
            return false;
        }

        return validateBasicCardFields(paymentDTO);
    }

    private boolean validateBasicCardFields(PaymentDTO paymentDTO) {
        // Validate card holder name
        if (paymentDTO.getCardHolderName() == null ||
                paymentDTO.getCardHolderName().trim().length() < 3) {
            log.warn("Invalid card holder name");
            return false;
        }

        // Validate expiry date
        if (paymentDTO.getExpiryDate() == null || !paymentDTO.getExpiryDate().matches("\\d{2}/\\d{2}")) {
            log.warn("Invalid expiry date format");
            return false;
        }

        // Parse and check if card is not expired
        try {
            String[] parts = paymentDTO.getExpiryDate().split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]) + 2000; // Assume 20XX

            LocalDateTime now = LocalDateTime.now();
            if (year < now.getYear() || (year == now.getYear() && month < now.getMonthValue())) {
                log.warn("Card is expired");
                return false;
            }
        } catch (Exception e) {
            log.warn("Error parsing expiry date: {}", e.getMessage());
            return false;
        }

        // Validate CVV
        if (paymentDTO.getCvv() == null || !paymentDTO.getCvv().matches("\\d{3,4}")) {
            log.warn("Invalid CVV");
            return false;
        }

        return true;
    }

    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    private boolean validateUPIPayment(PaymentDTO paymentDTO) {
        if (paymentDTO.getUpiId() == null || paymentDTO.getUpiId().trim().isEmpty()) {
            log.warn("UPI ID is missing");
            return false;
        }

        // UPI ID format: username@provider
        String upiId = paymentDTO.getUpiId().trim();

        // Basic UPI validation
        if (!upiId.contains("@")) {
            log.warn("UPI ID must contain @ symbol");
            return false;
        }

        String[] parts = upiId.split("@");
        if (parts.length != 2) {
            log.warn("Invalid UPI ID format");
            return false;
        }

        String username = parts[0];
        String provider = parts[1];

        // Username should not be empty
        if (username.isEmpty()) {
            log.warn("UPI username is empty");
            return false;
        }

        // Provider should be a valid UPI provider
        List<String> validProviders = Arrays.asList(
                "okhdfcbank", "oksbi", "okicici", "okaxis", "okpnb",
                "okyesbank", "okcanarabank", "okunionbank", "okindus",
                "okkotak", "okidbi", "okrbl", "ybl", "axl", "ibl",
                "apl", "paytm", "phonepe", "gpay", "bhim"
        );

        boolean validProvider = validProviders.stream()
                .anyMatch(provider::equalsIgnoreCase);

        if (!validProvider) {
            log.warn("Invalid UPI provider: {}", provider);
            // Still return true for demo purposes, but log warning
        }

        return true;
    }

    private boolean validateNetBankingPayment(PaymentDTO paymentDTO) {
        // For net banking, we just need to ensure a bank is selected
        if (paymentDTO.getBank() == null || paymentDTO.getBank().trim().isEmpty()) {
            log.warn("Bank selection is missing for net banking");
            return false;
        }

        // List of supported banks
        List<String> supportedBanks = Arrays.asList(
                "SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "PNB",
                "BOB", "CANARA", "UNION", "YESBANK", "INDUS"
        );

        if (!supportedBanks.contains(paymentDTO.getBank())) {
            log.warn("Unsupported bank: {}", paymentDTO.getBank());
            return false;
        }

        return true;
    }

    @Override
    public Ticket processRefund(String ticketId) {
        log.info("Processing refund for ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (!"PAID".equals(ticket.getPaymentStatus())) {
            throw new RuntimeException("No payment found for this ticket");
        }

        if ("CANCELLED".equals(ticket.getTicketStatus().toString())) {
            throw new RuntimeException("Ticket is already cancelled");
        }

        // Calculate refund based on cancellation policy
        double refundAmount = bookingService.calculateRefundAmount(ticket.getTotalFare(), ticket.getJourneyDate());

        // Generate refund transaction ID
        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setCancellationDate(LocalDateTime.now());
        ticket.setRefundAmount(refundAmount);
        ticket.setRefundStatus("PROCESSING");
        ticket.setRefundTransactionId(refundId);
        ticket.setPaymentStatus("REFUNDED");

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Refund initiated for ticket: {}, amount: {}, refund ID: {}",
                ticketId, refundAmount, refundId);

        // Send cancellation email
        try {
            emailService.sendCancellationEmail(
                    savedTicket.getUser(),
                    savedTicket.getTicketId(),
                    refundAmount
            );
        } catch (Exception e) {
            log.error("Failed to send cancellation email: {}", e.getMessage());
        }

        return savedTicket;
    }
}