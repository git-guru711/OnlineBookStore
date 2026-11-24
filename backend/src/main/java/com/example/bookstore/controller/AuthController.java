package com.example.bookstore.controller;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.model.RegisterRequest;
import com.example.bookstore.model.UserResponse;
import com.example.bookstore.service.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/auth/user")).body(user);
    }

    /** Used by the frontend as "login": 200 means the Basic credentials are valid. */
    @GetMapping("/user")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return authService.getCurrentUser(currentUser.id());
    }
}
