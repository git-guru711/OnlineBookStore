package com.example.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** The H2 console is a servlet outside Spring MVC, so these tests use a real port instead of MockMvc. */
class H2ConsoleIT {

    private static HttpResponse<String> get(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dev")
    class InTheDevProfile {

        @LocalServerPort
        private int port;

        @Test
        void theConsoleLoginPageIsServed_andMayUseFramesFromTheSameOrigin() throws Exception {
            HttpResponse<String> response = get(port, "/h2-console/");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("H2 Console");
            assertThat(response.headers().firstValue("X-Frame-Options"))
                    .hasValue("SAMEORIGIN"); // the console uses frames
        }

        @Test
        void theApiIsStillProtected() throws Exception {
            assertThat(get(port, "/api/v1/cart").statusCode()).isEqualTo(401);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    class WithoutTheDevProfile {

        @LocalServerPort
        private int port;

        @Test
        void thereIsNoConsole_theUrlIsDeniedLikeAnyUnknownPath() throws Exception {
            assertThat(get(port, "/h2-console/").statusCode()).isEqualTo(401);
        }
    }
}
