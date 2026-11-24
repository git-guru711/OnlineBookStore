package com.example.bookstore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_thenMeWithSameCredentials_returnsCurrentUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "  Jane.Doe@Example.com ", "password": "secret123", "fullName": "Jane Doe"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/auth/user"))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));

        mockMvc.perform(get("/api/v1/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, basic("jane.doe@example.com", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void me_withWrongPassword_returns401ProblemWithoutBrowserPopup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "wrong.pw@example.com", "password": "secret123", "fullName": "Wrong Password"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, basic("wrong.pw@example.com", "nope12345")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://bookstore.example/problems/unauthorized"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void demoUser_canLogIn() throws Exception {
        mockMvc.perform(get("/api/v1/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, basic("demo@example.com", "demo1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@example.com"))
                .andExpect(jsonPath("$.fullName").value("Demo User"));
    }

    static String basic(String username, String password) {
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
