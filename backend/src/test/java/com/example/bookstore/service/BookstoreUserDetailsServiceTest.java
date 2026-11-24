package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.entity.User;
import com.example.bookstore.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class BookstoreUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookstoreUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_knownEmail_returnsUserWithHashAndRole() {
        given(userRepository.findByEmail("jane@example.com"))
                .willReturn(Optional.of(new User("jane@example.com", "hashed", "Jane Doe")));

        UserDetails details = userDetailsService.loadUserByUsername("  Jane@Example.com ");

        assertThat(details).isInstanceOf(AuthenticatedUser.class);
        assertThat(details.getUsername()).isEqualTo("jane@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFound() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
