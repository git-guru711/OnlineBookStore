package com.example.bookstore.model;

import java.time.Instant;

/** Never contains the password hash. */
public record UserResponse(Long id, String email, String fullName, Role role, Instant createdAt) {}
