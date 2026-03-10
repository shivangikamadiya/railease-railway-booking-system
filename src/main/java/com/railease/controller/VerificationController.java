package com.railease.controller;

import com.railease.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final VerificationTokenService tokenService;

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token,
                              RedirectAttributes redirectAttributes) {

        log.info("Verifying email with token: {}", token);

        try {
            tokenService.verifyUser(token);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Email verified successfully! You can now login.");
            return "redirect:/login?verified=true";

        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login?error=true";
        }
    }
}