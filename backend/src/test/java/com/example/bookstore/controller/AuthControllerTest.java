package com.example.bookstore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.config.SecurityConfig;
import com.example.bookstore.exception.EmailAlreadyRegisteredException;
import com.example.bookstore.model.RegisterRequest;
import com.example.bookstore.model.Role;
import com.example.bookstore.model.UserResponse;
import com.example.bookstore.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class) // the real security rules
class AuthControllerTest {

    private static final String PROBLEMS = "https://bookstore.example/problems/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_validRequest_returns201WithLocationAndUser() throws Exception {
        given(authService.register(new RegisterRequest("jane@example.com", "secret123", "Jane Doe")))
                .willReturn(new UserResponse(
                        7L, "jane@example.com", "Jane Doe", Role.USER, Instant.parse("2026-11-20T10:15:30Z")));

        register("""
                {"email": "jane@example.com", "password": "secret123", "fullName": "Jane Doe"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/auth/user"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-20T10:15:30Z"));
    }

    @Test
    void register_emailAndNameWithSurroundingSpaces_areTrimmedBeforeValidation() throws Exception {
        given(authService.register(new RegisterRequest("Jane@Example.com", "secret123", "Jane Doe")))
                .willReturn(new UserResponse(7L, "jane@example.com", "Jane Doe", Role.USER, Instant.now()));

        register("""
                {"email": "  Jane@Example.com ", "password": "secret123", "fullName": " Jane Doe "}
                """).andExpect(status().isCreated());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        register("""
                {"email": "not-an-email", "password": "secret123", "fullName": "Jane"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        verifyNoInteractions(authService);
    }

    @Test
    void register_passwordTooShort_returns400() throws Exception {
        register("""
                {"email": "jane@example.com", "password": "abc12", "fullName": "Jane"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void register_passwordWithoutDigit_returns400() throws Exception {
        register("""
                {"email": "jane@example.com", "password": "onlyletters", "fullName": "Jane"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message").value("must contain at least one letter and one digit"));
    }

    @Test
    void register_blankFullName_returns400() throws Exception {
        register("""
                {"email": "jane@example.com", "password": "secret123", "fullName": "   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fullName"));
    }

    @Test
    void register_emptyBody_returns400ForEveryField() throws Exception {
        register("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[1].field").value("fullName"))
                .andExpect(jsonPath("$.errors[2].field").value("password"));
    }

    @Test
    void register_alreadyRegisteredEmail_returns409() throws Exception {
        given(authService.register(any())).willThrow(new EmailAlreadyRegisteredException("jane@example.com"));

        register("""
                {"email": "jane@example.com", "password": "secret123", "fullName": "Jane"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "email-already-registered"));
    }

    @Test
    void me_authenticated_returnsCurrentUser() throws Exception {
        given(authService.getCurrentUser(7L))
                .willReturn(new UserResponse(
                        7L, "jane@example.com", "Jane Doe", Role.USER, Instant.parse("2026-11-20T10:15:30Z")));
        AuthenticatedUser jane = new AuthenticatedUser(7L, "jane@example.com", "hash", Role.USER);

        mockMvc.perform(get("/api/v1/auth/user").with(user(jane)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void me_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "unauthorized"));
        verifyNoInteractions(authService);
    }

    private ResultActions register(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
