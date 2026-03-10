package com.railease.service;

import com.railease.entity.User;
import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface EmailService {

    void sendVerificationEmail(User user, String token, String siteURL)
            throws MessagingException, UnsupportedEncodingException;

    void sendBookingConfirmationEmail(User user, String ticketId, String trainName,
                                      String journeyDate, Double fare)
            throws MessagingException, UnsupportedEncodingException;

    void sendCancellationEmail(User user, String ticketId, Double refundAmount)
            throws MessagingException, UnsupportedEncodingException;

    void sendPasswordResetEmail(User user, String token, String siteURL)
            throws MessagingException, UnsupportedEncodingException;
}