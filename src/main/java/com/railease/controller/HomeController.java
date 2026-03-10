package com.railease.controller;

import com.railease.constants.UserRole;
import com.railease.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        
        if (user != null) {
            // If user is logged in, redirect to their respective dashboard
            if (user.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/user/dashboard";
            }
        }
        
        // If not logged in, show the public home page
        return "index";
    }

    @GetMapping("/home")
    public String userHome(HttpSession session) {
        User user = (User) session.getAttribute("activeUser");
        
        if (user != null) {
            // If user is logged in, redirect to their respective dashboard
            if (user.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/user/dashboard";
            }
        }
        
        // If not logged in, show the public home page
        return "redirect:/";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}