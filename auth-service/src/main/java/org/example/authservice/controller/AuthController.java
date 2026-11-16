package org.example.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.authservice.dto.ApiResponse;
import org.example.authservice.dto.request.UserRegistrationRequest;
import org.example.authservice.dto.response.UserResponse;
import org.example.authservice.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody UserRegistrationRequest request){
        authService.register(request);
        return ApiResponse.<Void>builder()
                .message("create user successfully")
                .build();
    }

    //login

    //reset password

    //lay accesstoken dua vao refreshtoken
}
