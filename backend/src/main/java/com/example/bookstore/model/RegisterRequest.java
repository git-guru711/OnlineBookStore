package com.example.bookstore.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,

        // 72 is the BCrypt input limit; longer passwords would be silently truncated.
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "must contain at least one letter and one digit")
        String password,

        @NotBlank @Size(max = 100) String fullName) {

    /** Trim before validation runs, so "  jane@example.com " is accepted. The password is kept as typed. */
    public RegisterRequest {
        email = email == null ? null : email.trim();
        fullName = fullName == null ? null : fullName.trim();
    }
}
