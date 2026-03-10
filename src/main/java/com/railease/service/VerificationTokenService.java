package com.railease.service;

import com.railease.entity.User;
import com.railease.entity.VerificationToken;

public interface VerificationTokenService {
    String createVerificationToken(User user);
    VerificationToken getVerificationToken(String token);
    void verifyUser(String token);
}