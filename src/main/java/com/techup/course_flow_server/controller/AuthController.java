package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.auth.AdminLoginResponse;
import com.techup.course_flow_server.dto.auth.AuthResponse;
import com.techup.course_flow_server.dto.auth.LoginRequest;
import com.techup.course_flow_server.dto.auth.RegisterRequest;
import com.techup.course_flow_server.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Explicit JSON keys ({@code access_token}, {@code refresh_token}) so clients never depend on
     * Jackson record naming.
     */
    @PostMapping("/admin-login")
    public Map<String, String> adminLogin(@Valid @RequestBody LoginRequest request) {
        AdminLoginResponse r = authService.adminLogin(request);
        return Map.of(
                "access_token", r.accessToken(),
                "refresh_token", r.refreshToken(),
                "user_id", r.userId(),
                "role", r.role());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}