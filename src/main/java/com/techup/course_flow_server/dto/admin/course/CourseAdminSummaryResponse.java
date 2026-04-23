package com.techup.course_flow_server.dto.admin.course;

import com.techup.course_flow_server.entity.Course;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Row data for the Course admin table. */
@Getter
@Builder
@AllArgsConstructor
public class CourseAdminSummaryResponse {

    private UUID id;
    private String title;
    /** Number of lessons (CourseModule) in this course. */
    private int lessonCount;
    private BigDecimal price;
    private String coverImageUrl;
    private Course.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
