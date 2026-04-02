package com.railease.controller;

import com.railease.constants.UserRole;
import com.railease.entity.PasswordResetToken;
import com.railease.entity.User;
import com.railease.repository.PasswordResetTokenRepository;
import com.railease.repository.UserRepository;
import com.railease.service.EmailService;
import com.railease.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                @RequestParam(value = "verified", required = false) String verified,
                                @RequestParam(value = "reset", required = false) String reset) {

        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        if (registered != null) {
            model.addAttribute("message", "Registration successful! Please check your email to verify your account.");
        }

        if (verified != null) {
            model.addAttribute("message", "Email verified successfully! You can now login.");
        }
        if (reset != null) {
            model.addAttribute("message", "Password reset successful! Please login with your new password.");
        }

        return "login";
    }






    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        log.info("Login attempt for user: {}", username);

        try {
            User user = loginService.authenticate(username, password);

            // Store user in session
            session.setAttribute("activeUser", user);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            log.info("User logged in successfully: {}", username);
            log.info("User role: {}", user.getRole());

            // Check both enum and string for safety
            boolean isAdmin = false;

            if (user.getRole() != null) {
                // Check if it's the ADMIN enum
                System.out.println("this is the user role:"+user.getRole());
                if (user.getRole() == UserRole.ADMIN) {
                    isAdmin = true;
                }
                // Also check if it's the string "ADMIN" (in case it's stored as string)
                else if (user.getRole().toString().equals("ADMIN")) {
                    isAdmin = true;
                }
            }

            if (isAdmin) {
                log.info("Redirecting to ADMIN dashboard");
                return "redirect:/admin/dashboard";
            } else {
                log.info("Redirecting to USER dashboard");
                return "redirect:/user/dashboard";
            }

        } catch (Exception e) {
            log.error("Login failed for user: {}", username, e);
            return "redirect:/login?error=true";
        }
    }
//
//    @PostMapping("/login")
//    public String login(@RequestParam String username,
//                        @RequestParam String password,
//                        HttpSession session,
//                        Model model) {
//
//        log.info("Login attempt for user: {}", username);
//
//        try {
//            User user = loginService.authenticate(username, password);
//
//            // Store user in session
//            session.setAttribute("activeUser", user);
//            session.setMaxInactiveInterval(30 * 60); // 30 minutes
//
//            log.info("User logged in successfully: {}", username);
//
//            // ✅ Role-based redirection logic
//            if (UserRole.ADMIN.equals(user.getRole())) {
//                return "redirect:/admin/dashboard";
//            } else {
//                return "redirect:/user/dashboard";
//            }
//
//        } catch (Exception e) {
//            log.error("Login failed for user: {}", username, e);
//            return "redirect:/login?error=true";
//        }
//    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                passwordResetTokenRepository.deleteByUserUserId(user.getUserId());

                String token = UUID.randomUUID().toString();
                PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                        .token(token)
                        .user(user)
                        .expiryDate(LocalDateTime.now().plusHours(24))
                        .used(false)
                        .build();

                passwordResetTokenRepository.save(passwordResetToken);

                String appUrl = request.getScheme() + "://" + request.getServerName() + ":" +
                        request.getServerPort() + request.getContextPath();

                emailService.sendPasswordResetEmail(user, token, appUrl);
                log.info("Password reset email sent to: {}", email);
            } else {
                log.warn("Password reset requested for non-existent email: {}", email);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "If an account exists for this email, password reset instructions have been sent.");
            return "redirect:/forgot-password";
        } catch (Exception e) {
            log.error("Failed to process forgot password request for email: {}", email, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to process your request right now. Please try again.");
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        if (token == null || token.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Invalid password reset link.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }

        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByTokenWithUser(token);
        if (!tokenOptional.isPresent()) {
            model.addAttribute("errorMessage", "Invalid password reset link.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }

        PasswordResetToken resetToken = tokenOptional.get();
        if (resetToken.isExpired() || resetToken.isUsed()) {
            model.addAttribute("errorMessage", "This password reset link is expired or already used.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model) {
        model.addAttribute("token", token);

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "reset-password";
        }

        if (password.length() < 6) {
            model.addAttribute("errorMessage", "Password must be at least 6 characters.");
            return "reset-password";
        }

        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByTokenWithUser(token);
        if (!tokenOptional.isPresent()) {
            model.addAttribute("errorMessage", "Invalid password reset link.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }

        PasswordResetToken resetToken = tokenOptional.get();
        if (resetToken.isExpired() || resetToken.isUsed()) {
            model.addAttribute("errorMessage", "This password reset link is expired or already used.");
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
        return "redirect:/login?reset=true";
    }
}
