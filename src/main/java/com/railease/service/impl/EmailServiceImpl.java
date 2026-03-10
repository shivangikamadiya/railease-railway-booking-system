package com.railease.service.impl;

import com.railease.entity.User;
import com.railease.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(User user, String token, String siteURL)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = fromEmail;
        String senderName = "RailEase Support";
        String subject = "Please verify your registration";

        String verifyURL = siteURL + "/verify-email?token=" + token;

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e9ecef; border-radius: 10px;'>"
                + "<h2 style='color: #4a6cf7; text-align: center;'>Welcome to RailEase! 🚂</h2>"
                + "<p>Dear <strong>" + user.getFullName() + "</strong>,</p>"
                + "<p>Thank you for registering with RailEase. Please verify your email address by clicking the button below:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href='" + verifyURL + "' style='background: linear-gradient(135deg, #4a6cf7 0%, #3a5cdb 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>Verify Email</a>"
                + "</div>"
                + "<p>Or copy and paste this link into your browser:</p>"
                + "<p style='background: #f8f9fc; padding: 10px; border-radius: 5px; word-break: break-all;'>" + verifyURL + "</p>"
                + "<p>This link will expire in 24 hours.</p>"
                + "<p>If you didn't create an account with RailEase, please ignore this email.</p>"
                + "<p>Regards,<br>RailEase Team</p>"
                + "<hr style='border: 1px solid #e9ecef;'>"
                + "<p style='color: #6c757d; font-size: 12px; text-align: center;'>This is an automated message, please do not reply.</p>"
                + "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
        log.info("Verification email sent to: {}", toAddress);
    }

    @Override
    public void sendBookingConfirmationEmail(User user, String ticketId, String trainName,
                                             String journeyDate, Double fare)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = fromEmail;
        String senderName = "RailEase Bookings";
        String subject = "Booking Confirmation - Ticket ID: " + ticketId;

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e9ecef; border-radius: 10px;'>"
                + "<h2 style='color: #28a745; text-align: center;'>Booking Confirmed! ✅</h2>"
                + "<p>Dear <strong>" + user.getFullName() + "</strong>,</p>"
                + "<p>Your ticket has been successfully booked. Here are the details:</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>"
                + "<tr><td style='padding: 10px; background: #f8f9fc;'><strong>Ticket ID:</strong></td>"
                + "<td style='padding: 10px;'>" + ticketId + "</td></tr>"
                + "<tr><td style='padding: 10px;'><strong>Train:</strong></td>"
                + "<td style='padding: 10px;'>" + trainName + "</td></tr>"
                + "<tr><td style='padding: 10px; background: #f8f9fc;'><strong>Journey Date:</strong></td>"
                + "<td style='padding: 10px;'>" + journeyDate + "</td></tr>"
                + "<tr><td style='padding: 10px;'><strong>Total Fare:</strong></td>"
                + "<td style='padding: 10px; color: #28a745; font-weight: bold;'>₹" + fare + "</td></tr>"
                + "</table>"
                + "<p>You can view your ticket details by logging into your account.</p>"
                + "<p>Safe journey! ✨</p>"
                + "<p>Regards,<br>RailEase Team</p>"
                + "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
        log.info("Booking confirmation email sent to: {}", toAddress);
    }

    @Override
    public void sendCancellationEmail(User user, String ticketId, Double refundAmount)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = fromEmail;
        String senderName = "RailEase Support";
        String subject = "Ticket Cancellation - Ticket ID: " + ticketId;

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e9ecef; border-radius: 10px;'>"
                + "<h2 style='color: #dc3545; text-align: center;'>Ticket Cancelled</h2>"
                + "<p>Dear <strong>" + user.getFullName() + "</strong>,</p>"
                + "<p>Your ticket with ID <strong>" + ticketId + "</strong> has been successfully cancelled.</p>"
                + "<p style='font-size: 18px; text-align: center;'>Refund Amount: <span style='color: #28a745; font-weight: bold;'>₹" + refundAmount + "</span></p>"
                + "<p>The refund will be processed to your original payment method within 5-7 business days.</p>"
                + "<p>We hope to serve you again soon!</p>"
                + "<p>Regards,<br>RailEase Team</p>"
                + "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
        log.info("Cancellation email sent to: {}", toAddress);
    }

    @Override
    public void sendPasswordResetEmail(User user, String token, String siteURL)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = fromEmail;
        String senderName = "RailEase Support";
        String subject = "Password Reset Request";

        String resetURL = siteURL + "/reset-password?token=" + token;

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e9ecef; border-radius: 10px;'>"
                + "<h2 style='color: #4a6cf7; text-align: center;'>Password Reset</h2>"
                + "<p>Dear <strong>" + user.getFullName() + "</strong>,</p>"
                + "<p>We received a request to reset your password.</p>"
                + "<p>Click the button below to reset your password:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href='" + resetURL + "' style='background: linear-gradient(135deg, #4a6cf7 0%, #3a5cdb 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>Reset Password</a>"
                + "</div>"
                + "<p>Or copy and paste this link:</p>"
                + "<p style='background: #f8f9fc; padding: 10px; border-radius: 5px; word-break: break-all;'>" + resetURL + "</p>"
                + "<p>This link will expire in 24 hours.</p>"
                + "<p>If you didn't request this, please ignore this email.</p>"
                + "<p>Regards,<br>RailEase Team</p>"
                + "</div>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
        log.info("Password reset email sent to: {}", toAddress);
    }
}