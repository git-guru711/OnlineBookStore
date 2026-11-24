package com.example.bookstore.service;

import com.example.bookstore.entity.User;
import com.example.bookstore.exception.EmailAlreadyRegisteredException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.RegisterRequest;
import com.example.bookstore.model.UserResponse;
import com.example.bookstore.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User user = userRepository.saveAndFlush(
                new User(email, passwordEncoder.encode(request.password()), request.fullName()));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository
                .findById(userId)
                .map(AuthService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User %d not found".formatted(userId)));
    }

    /** Emails are compared case-insensitively, so they are stored trimmed and lowercased. */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
    }
}
