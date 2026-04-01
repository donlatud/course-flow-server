package com.techup.course_flow_server.service;

import com.techup.course_flow_server.config.SupabaseProperties;
import com.techup.course_flow_server.dto.auth.AuthResponse;
import com.techup.course_flow_server.dto.auth.LoginRequest;
import com.techup.course_flow_server.dto.auth.RegisterRequest;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    private final SupabaseProperties supabaseProperties;
    private final UserService userService;
    private final RestTemplate restTemplate;

    public AuthService(SupabaseProperties supabaseProperties, UserService userService) {
        this.supabaseProperties = supabaseProperties;
        this.userService = userService;
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseProperties.apiKey());
        return headers;
    }

    public AuthResponse login(LoginRequest request) {
        String url = supabaseProperties.url() + "/auth/v1/token?grant_type=password";
        Map<String, String> body = Map.of(
            "email", request.email(),
            "password", request.password()
        );
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers());
        Map response = restTemplate.postForObject(url, entity, Map.class);
        return new AuthResponse(
            (String) response.get("access_token"),
            (String) response.get("refresh_token"),
            (String) response.get("token_type"),
            Long.valueOf(response.get("expires_in").toString())
        );
    }

    public AuthResponse register(RegisterRequest request) {
        // 1. สร้าง user ใน Supabase Auth
        String url = supabaseProperties.url() + "/auth/v1/signup";
        Map<String, String> body = Map.of(
            "email", request.email(),
            "password", request.password()
        );
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers());
        Map response = restTemplate.postForObject(url, entity, Map.class);

        // 2. ดึง supabase user id
        Map userMap = (Map) response.get("user");
        String supabaseUserId = (String) userMap.get("id");

        // 3. บันทึกข้อมูลเพิ่มเติมลง DB
        userService.createUser(request, supabaseUserId);

        return new AuthResponse(
            (String) response.get("access_token"),
            (String) response.get("refresh_token"),
            (String) response.get("token_type"),
            response.get("expires_in") != null
                ? Long.valueOf(response.get("expires_in").toString()) : null
        );
    }
}