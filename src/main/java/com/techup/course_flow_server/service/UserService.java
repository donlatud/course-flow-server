package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.auth.RegisterRequest;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        User user = User.builder()
            .id(userId)
            .email(request.email())
            .passwordHash("supabase_managed")
            .fullName(request.fullName())
            .age(request.age())
            .educationalBackground(request.educationalBackground())
            .build();

        return userRepository.save(user);
    }
}