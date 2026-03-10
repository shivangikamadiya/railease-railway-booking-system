package com.railease.controller;

import com.railease.dto.UserRegistrationDTO;
import com.railease.entity.User;
import com.railease.service.EmailService;
import com.railease.service.UserService;
import com.railease.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final UserService userService;
    private final EmailService emailService;
    private final VerificationTokenService tokenService;

    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
        return "register";
    }

    @PostMapping
    public String registerUser(@Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO registrationDTO,
                               BindingResult result,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {

        log.info("Processing registration for user: {}", registrationDTO.getEmail());

        // Check for validation errors
        if (result.hasErrors()) {
            log.warn("Validation errors during registration: {}", result.getAllErrors());
            return "register";
        }

        // Check if passwords match
        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            log.warn("Passwords do not match!");
            model.addAttribute("passwordError", "Passwords do not match!");
            return "register";
        }

        try {
            // Check if username already exists
            log.info("Checking if username exists: {}", registrationDTO.getUsername());
            if (userService.existsByUsername(registrationDTO.getUsername())) {
                log.warn("Username already exists: {}", registrationDTO.getUsername());
                model.addAttribute("usernameError", "Username already exists!");
                return "register";
            }

            // Check if email already exists
            log.info("Checking if email exists: {}", registrationDTO.getEmail());
            if (userService.existsByEmail(registrationDTO.getEmail())) {
                log.warn("Email already exists: {}", registrationDTO.getEmail());
                model.addAttribute("emailError", "Email already registered!");
                return "register";
            }

            // Register the user
            log.info("Calling userService.registerUser...");
            User registeredUser = userService.registerUser(registrationDTO);
            log.info("User registered successfully with ID: {}", registeredUser.getUserId());

            // Create verification token
            log.info("Creating verification token...");
            String token = tokenService.createVerificationToken(registeredUser);
            log.info("Token created: {}", token);

            // Send verification email
            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" +
                    request.getServerPort() + request.getContextPath();
            log.info("App URL: {}", appUrl);

            emailService.sendVerificationEmail(registeredUser, token, appUrl);
            log.info("Verification email sent successfully");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Please check your email to verify your account.");

            return "redirect:/login?registered";

        } catch (IOException e) {
            log.error("IO Error during registration: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error uploading profile photo: " + e.getMessage());
            return "register";
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
}