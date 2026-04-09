package com.techup.course_flow_server.dto.enrollment;

import com.techup.course_flow_server.entity.Enrollment;
import java.math.BigDecimal;
import java.util.UUID;

public record EnrollmentResponse(
    UUID enrollmentId,
    UUID courseId,
    String courseTitle,
    String courseDescription,
    String coverImageUrl,  
    BigDecimal progressPercentage,
    Enrollment.Status status,
    Integer totalHours,    
    Long lessonCount
) {}