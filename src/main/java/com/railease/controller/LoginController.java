package com.railease.controller;

import com.railease.constants.UserRole;
import com.railease.entity.User;
import com.railease.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;

    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                @RequestParam(value = "verified", required = false) String verified) {

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
}