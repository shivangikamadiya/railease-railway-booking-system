package com.railease.service;

import com.railease.entity.User;

public interface LoginService {
    User authenticate(String username, String password);
}