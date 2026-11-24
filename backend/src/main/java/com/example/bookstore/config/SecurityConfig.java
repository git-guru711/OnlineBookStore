package com.example.bookstore.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Stateless HTTP Basic, deny by default. The H2 console is open only in the dev profile. */
@Configuration
public class SecurityConfig {

    private static final String H2_CONSOLE = "/h2-console/**";

    private static final String[] API_DOCS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthenticationEntryPoint entryPoint, Environment environment) throws Exception {
        boolean devProfile = environment.acceptsProfiles(Profiles.of("dev"));

        http.csrf(AbstractHttpConfigurer::disable) // no cookies/sessions, so no CSRF risk
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/books", "/api/v1/books/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                            .permitAll()
                            .requestMatchers("/error")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, API_DOCS)
                            .permitAll();
                    if (devProfile) {
                        auth.requestMatchers(H2_CONSOLE).permitAll(); // dev only; the console has its own login
                    }
                    auth.anyRequest().authenticated();
                })
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));

        if (devProfile) {
            // the H2 console uses frames
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }
        return http.build();
    }

    /** Sends 401s through the exception handler: same problem JSON, and no WWW-Authenticate header (no browser popup). */
    @Bean
    AuthenticationEntryPoint problemAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return (request, response, exception) -> resolver.resolveException(request, response, null, exception);
    }
}
