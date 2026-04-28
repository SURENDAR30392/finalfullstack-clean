package com.fullstack.backend.controller;

import com.fullstack.backend.exception.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentProxyController {

    private static final int TIMEOUT_SECONDS = 12;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyAssignment(@RequestParam("url") String url) {
        URI target = parseUrl(url);
        if (!"http".equalsIgnoreCase(target.getScheme()) && !"https".equalsIgnoreCase(target.getScheme())) {
            throw new BadRequestException("Only http/https assignment links are allowed.");
        }

        HttpRequest request = HttpRequest.newBuilder(target)
                .header(HttpHeaders.USER_AGENT, "LMS-Assignment-Proxy")
                .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse("text/html");
            return ResponseEntity.status(response.statusCode())
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to load assignment link.");
        }
    }

    private URI parseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("Assignment link is required.");
        }
        try {
            return new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid assignment link.");
        }
    }
}
