package com.railease.service.impl;

import com.railease.constants.UserRole;
import com.railease.dto.UserRegistrationDTO;
import com.railease.entity.User;
import com.railease.exception.UserNotFoundException;
import com.railease.repository.UserRepository;
import com.railease.service.UserService;
import com.railease.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public boolean existsByUsername(String username) {
        log.debug("Checking if username exists: {}", username);
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        log.debug("Checking if email exists: {}", email);
        return userRepository.existsByEmail(email);
    }

    @Override
    public User registerUser(UserRegistrationDTO registrationDTO) throws IOException {
        log.info("Registering new user: {}", registrationDTO.getUsername());

        // Check if username or email already exists
        if (existsByUsername(registrationDTO.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (existsByEmail(registrationDTO.getEmail())) {
            throw new RuntimeException("Email already registered!");
        }

        User user = User.builder()
                .username(registrationDTO.getUsername())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .fullName(registrationDTO.getFullName())
                .email(registrationDTO.getEmail())
                .mobileNumber(registrationDTO.getMobileNumber())
                .role(UserRole.CUSTOMER)
                .isEnabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Handle profile photo
        if (registrationDTO.getProfilePhoto() != null && !registrationDTO.getProfilePhoto().isEmpty()) {
            user.setProfilePhoto(registrationDTO.getProfilePhoto().getBytes());
            user.setProfilePhotoContentType(registrationDTO.getProfilePhoto().getContentType());
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());
        return savedUser;
    }

    @Override
    public User authenticateUser(String username, String password) {
        log.info("Authenticating user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        if (!user.getIsEnabled()) {
            throw new RuntimeException("Email not verified. Please check your email to verify your account.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User authenticated successfully: {}", user.getUserId());
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User findById(Long userId) throws UserNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Override
    public User updateProfile(User user) {
        log.info("Updating profile for user: {}", user.getUserId());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User updateProfilePhoto(Long userId, MultipartFile photo) throws IOException {
        User user = findById(userId);

        if (photo != null && !photo.isEmpty()) {
            user.setProfilePhoto(photo.getBytes());
            user.setProfilePhotoContentType(photo.getContentType());
            user.setUpdatedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    @Override
    public User changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public void enableUser(String email) {
        log.info("Enabling user with email: {}", email);
        userRepository.enableUser(email);
    }

    @Override
    public void storeUserSession(Long userId, String sessionId) {
        User user = findById(userId);
        user.setSessionId(sessionId);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}