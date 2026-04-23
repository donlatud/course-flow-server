package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.user.UpdateUserRequest;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.repository.UserRepository;
import com.techup.course_flow_server.service.UserService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/me")
    public User getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PutMapping("/me")
    public User updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userService.updateUser(userId, request);
    }
}