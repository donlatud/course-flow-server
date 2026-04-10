package com.techup.course_flow_server.dto.user;

import java.time.LocalDate;

public record UpdateUserRequest(
    String fullName,
    String educationalBackground,
    String profilePictureUrl,
    LocalDate dateOfBirth
) {}