package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.auth.RegisterRequest;
import com.techup.course_flow_server.dto.user.UpdateUserRequest;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.repository.UserRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(RegisterRequest request, String supabaseUserId) {
        UUID userId = UUID.fromString(supabaseUserId);

        if (userRepository.existsById(userId)) {
            return userRepository.findById(userId).get();
        }

        int age = request.dateOfBirth() != null
            ? Period.between(request.dateOfBirth(), LocalDate.now()).getYears()
            : 0;

        User user = User.builder()
            .id(userId)
            .email(request.email())
            .passwordHash("supabase_managed")
            .fullName(request.fullName())
            .dateOfBirth(request.dateOfBirth())
            .age(age)
            .educationalBackground(request.educationalBackground())
            .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.educationalBackground() != null) user.setEducationalBackground(request.educationalBackground());
        if (request.profilePictureUrl() != null) user.setProfilePictureUrl(request.profilePictureUrl());
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
            user.setAge(Period.between(request.dateOfBirth(), LocalDate.now()).getYears());
        }

        return userRepository.save(user);
    }
}