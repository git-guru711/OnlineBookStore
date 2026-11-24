package com.example.bookstore.config;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The logged-in user. Carries the id, so services need no lookup by email. */
public record AuthenticatedUser(Long id, String email, String passwordHash, Role role) implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Keeps the password hash out of logs. */
    @Override
    public String toString() {
        return "AuthenticatedUser[id=%d, email=%s, role=%s]".formatted(id, email, role);
    }
}
