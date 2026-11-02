package com.kopnus.jeremiah_recruitment_test.controller;

import com.kopnus.jeremiah_recruitment_test.config.RateLimitProperties;
import com.kopnus.jeremiah_recruitment_test.dto.LoginRequest;
import com.kopnus.jeremiah_recruitment_test.dto.LoginResponse;
import com.kopnus.jeremiah_recruitment_test.security.AuthenticatedUser;
import com.kopnus.jeremiah_recruitment_test.security.RateLimiter;
import com.kopnus.jeremiah_recruitment_test.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public AuthController(AuthService authService,
                          RateLimiter rateLimiter,
                          RateLimitProperties rateLimitProperties) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,  HttpServletRequest httpRequest) {
        String key = "login:" + httpRequest.getRemoteAddr() + ":" + request.username().toLowerCase(Locale.ROOT);
        rateLimiter.consume(key, rateLimitProperties.login());
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }

}
