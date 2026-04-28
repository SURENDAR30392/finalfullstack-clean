package com.fullstack.backend.service;

import com.fullstack.backend.dto.AuthResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final String frontendBaseUrl;

    public GoogleOAuthSuccessHandler(
            UserService userService,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.userService = userService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String requestedRole = getCookieValue(request, "oauth_role");

        try {
            AuthResponse authResponse = userService.completeGoogleLogin(name, email, requestedRole);
            clearCookie(response, "oauth_role");

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth-success")
                    .queryParam("token", authResponse.token())
                    .queryParam("id", authResponse.user().id())
                    .queryParam("name", authResponse.user().name())
                    .queryParam("email", authResponse.user().email())
                    .queryParam("role", authResponse.user().role())
                    .queryParam("provider", authResponse.user().provider())
                    .build()
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (Exception exception) {
            clearCookie(response, "oauth_role");
            String error = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendBaseUrl + "/login?oauthError=" + error);
        }
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
