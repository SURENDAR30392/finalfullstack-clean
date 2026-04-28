package com.fullstack.backend.config;

import com.fullstack.backend.service.GoogleOAuthSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtFilter jwtFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            GoogleOAuthSuccessHandler googleOAuthSuccessHandler,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth

                        // ✅ Allow frontend (IMPORTANT)
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/static/**",
                                "/favicon.ico",
                                "/*.js",
                                "/*.css"
                        ).permitAll()

                        // ✅ Allow auth APIs
                        .requestMatchers("/api/auth/**").permitAll()

                        // 🔒 Secure other APIs
                        .requestMatchers("/api/**").authenticated()

                        // ✅ Everything else allowed (frontend routing)
                        .anyRequest().permitAll()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .oauth2Login(oauth -> oauth
                        .successHandler(googleOAuthSuccessHandler)
                        .failureHandler((request, response, exception) ->
                                redirectWithError(response, frontendBaseUrl, exception.getMessage()))
                )

                .build();
    }

    private void redirectWithError(HttpServletResponse response,
                                   String frontendBaseUrl,
                                   String message) throws IOException, ServletException {

        String encodedMessage = URLEncoder.encode(
                message == null ? "Google login failed." : message,
                StandardCharsets.UTF_8
        );

        response.sendRedirect(frontendBaseUrl + "/login?oauthError=" + encodedMessage);
    }
}