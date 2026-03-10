package com.railease.service;

import com.railease.dto.UserRegistrationDTO;
import com.railease.entity.User;
import com.railease.exception.UserNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface UserService {

    // Registration methods
    User registerUser(UserRegistrationDTO registrationDTO) throws IOException;

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Authentication methods
    User authenticateUser(String username, String password);

    // User lookup methods
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User findById(Long userId) throws UserNotFoundException;

    List<User> getAllUsers();

    // Profile management
    User updateProfile(User user);

    User updateProfilePhoto(Long userId, MultipartFile photo) throws IOException;

    User changePassword(Long userId, String oldPassword, String newPassword);

    // Account management
    void enableUser(String email);

    void storeUserSession(Long userId, String sessionId);
}