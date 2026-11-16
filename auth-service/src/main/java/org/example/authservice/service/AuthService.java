package org.example.authservice.service;

import org.example.authservice.dto.request.UserRegistrationRequest;
import org.example.authservice.dto.response.UserResponse;

public interface AuthService {
    void register(UserRegistrationRequest request);
}
