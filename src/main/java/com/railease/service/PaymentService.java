package com.railease.service;

import com.railease.dto.PaymentDTO;
import com.railease.entity.Ticket;
import org.springframework.stereotype.Service;

@Service
public interface PaymentService {

    Ticket processPayment(PaymentDTO paymentDTO);

    boolean validatePayment(PaymentDTO paymentDTO);

    Ticket processRefund(String ticketId);
}