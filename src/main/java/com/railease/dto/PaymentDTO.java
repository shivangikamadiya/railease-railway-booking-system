package com.railease.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class
PaymentDTO {

    @NotBlank(message = "Ticket ID is required")
    private String ticketId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    // Card fields
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;

    // UPI fields
    private String upiId;

    // Net Banking fields
    private String bank;

    // Helper method to mask card number for display
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String clean = cardNumber.replaceAll("\\s", "");
        int len = clean.length();
        return "XXXX-XXXX-XXXX-" + clean.substring(len - 4);
    }
}