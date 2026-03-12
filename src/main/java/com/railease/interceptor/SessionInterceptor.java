package com.railease.interceptor;

import com.railease.constants.UserRole;
import com.railease.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        // Allow access to public pages
        String uri = request.getRequestURI();
        if (uri.contains("/login") ||
                uri.contains("/register") ||
                uri.contains("/verify-email") ||
                uri.contains("/css/") ||
                uri.contains("/js/") ||
                uri.contains("/images/") ||
                uri.contains("/webjars/") ||
                uri.equals("/RailEase/") ||
                uri.equals("/RailEase") ||
                uri.equals("/RailEase/home") ||
                uri.equals("/home") ||
                uri.contains("/booking/confirmation/") ||
                uri.contains("/booking/payment/") ||
                uri.contains("/booking/success/") ||
                uri.contains("/booking/download-pdf/") ||
                uri.contains("/booking/view/")) {
            return true;
        }

        // Check if user is logged in
        if (session == null || session.getAttribute("activeUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        // Check role-based access - Fixed: compare enum properly
        User user = (User) session.getAttribute("activeUser");
        if (uri.contains("/admin/") && user.getRole() != UserRole.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/user/dashboard");
            return false;
        }

        return true;
    }
}