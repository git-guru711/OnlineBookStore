package com.example.bookstore.service;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookstoreUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public BookstoreUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository
                .findByEmail(AuthService.normalizeEmail(username))
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
