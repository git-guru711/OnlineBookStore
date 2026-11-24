package com.example.bookstore.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalog_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/books")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/books/1")).andExpect(status().isOk());
    }

    @Test
    void register_isPublic_andNeedsNoCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "public@example.com", "password": "secret123", "fullName": "Public"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpoint_withoutCredentials_returns401ProblemWithoutBrowserPopup() throws Exception {
        mockMvc.perform(get("/api/v1/auth/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://bookstore.example/problems/unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/user"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void unknownRoute_withoutCredentials_isDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/not-mapped")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflight_forProtectedEndpoint_isAllowedWithoutCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/auth/user")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void publicCalls_doNotCreateSessions() throws Exception {
        mockMvc.perform(get("/api/v1/books")).andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
