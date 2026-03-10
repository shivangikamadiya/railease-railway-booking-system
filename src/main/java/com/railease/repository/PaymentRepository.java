package com.railease.repository;

import com.railease.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserUserId(Long userId);

    List<Payment> findByTicketTicketId(String ticketId);

    List<Payment> findByPaymentStatus(String paymentStatus);
}