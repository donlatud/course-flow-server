package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.enrollment.EnrollmentResponse;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<EnrollmentResponse> getMyEnrollments(UUID userId) {
        
        return enrollmentRepository.findEnrollmentResponsesByUserId(userId);
    }
}