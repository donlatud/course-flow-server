package com.techup.course_flow_server.dto.admin.course;

import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.Material;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Full course detail returned after create or GET by id. */
@Getter
@Builder
@AllArgsConstructor
public class CourseAdminDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private String detail;
    private BigDecimal price;
    private Integer totalLearningTime;
    private String coverImageUrl;
    private String trailerVideoUrl;
    private String attachmentUrl;
    private Course.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ModuleResponse> modules;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ModuleResponse {
        private UUID id;
        private String title;
        private int orderIndex;
        private List<SubLessonResponse> subLessons;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SubLessonResponse {
        private UUID id;
        private String title;
        private Material.FileType fileType;
        private String detail;
        /** Stored as file_url in DB (Supabase public URL, etc.). */
        private String fileUrl;
        private int orderIndex;
    }
}
