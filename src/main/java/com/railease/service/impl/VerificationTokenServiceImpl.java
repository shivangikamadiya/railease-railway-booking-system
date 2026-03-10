package com.railease.service.impl;

import com.railease.entity.User;
import com.railease.entity.VerificationToken;
import com.railease.exception.UserNotFoundException;
import com.railease.repository.UserRepository;
import com.railease.repository.VerificationTokenRepository;
import com.railease.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Override
    public String createVerificationToken(User user) {
        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationToken.setUsed(false);

        tokenRepository.save(verificationToken);
        log.info("Created verification token for user: {}", user.getEmail());

        return token;
    }

    @Override
    public VerificationToken getVerificationToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
    }

    @Override
    public void verifyUser(String token) {
        VerificationToken verificationToken = getVerificationToken(token);

        if (verificationToken.getUsed()) {
            throw new RuntimeException("Token already used");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = verificationToken.getUser();
        user.setIsEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("User verified successfully: {}", user.getEmail());
    }
}