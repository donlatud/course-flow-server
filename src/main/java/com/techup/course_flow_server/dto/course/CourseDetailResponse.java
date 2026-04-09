package com.techup.course_flow_server.dto.course;

import com.techup.course_flow_server.dto.module.ModuleWithMaterialsResponse;
import com.techup.course_flow_server.entity.Course;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CourseDetailResponse {
    private UUID id;
    private String title;
    private String description;
    private String detail;
    private BigDecimal price;
    private String category;
    private String subject;
    private Course.Status status;
    private UUID adminId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String coverImageUrl;
    private String trailerVideoUrl;
    private String attachmentUrl;
    private Integer totalLearningTime;
    private Integer lessonCount;
    private List<ModuleWithMaterialsResponse> modules;
}
