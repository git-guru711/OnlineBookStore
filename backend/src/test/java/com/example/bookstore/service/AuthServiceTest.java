package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.bookstore.entity.User;
import com.example.bookstore.exception.EmailAlreadyRegisteredException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.RegisterRequest;
import com.example.bookstore.model.UserResponse;
import com.example.bookstore.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_storesHashedPassword_andReturnsUser() {
        given(passwordEncoder.encode("secret123")).willReturn("hashed-secret");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(new RegisterRequest("jane@example.com", "secret123", "Jane Doe"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.fullName()).isEqualTo("Jane Doe");
    }

    @Test
    void register_normalizesEmailToTrimmedLowercase() {
        given(passwordEncoder.encode(any())).willReturn("hashed");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserResponse response =
                authService.register(new RegisterRequest("  Jane.Doe@Example.COM ", "secret123", "Jane"));

        assertThat(response.email()).isEqualTo("jane.doe@example.com");
        verify(userRepository).existsByEmail("jane.doe@example.com");
    }

    @Test
    void register_alreadyRegisteredEmail_throwsConflict_andSavesNothing() {
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Dup@Example.com", "secret123", "Dup")))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email dup@example.com is already registered");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void getCurrentUser_returnsUser() {
        given(userRepository.findById(7L)).willReturn(Optional.of(new User("jane@example.com", "hash", "Jane Doe")));

        assertThat(authService.getCurrentUser(7L).email()).isEqualTo("jane@example.com");
    }

    @Test
    void getCurrentUser_deletedUser_throwsNotFound() {
        given(userRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(7L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
