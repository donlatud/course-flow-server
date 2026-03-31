package com.techup.course_flow_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String title;

    /**
     * Short description / summary shown in course card and header.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Full course detail text (syllabus, long copy).
     */
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String category;

    private String subject;

    /**
     * Total learning time in hours (e.g. 6, 12).
     */
    @Column(name = "total_learning_time")
    private Integer totalLearningTime;

    /**
     * Public URL of the course cover image.
     */
    @Column(name = "cover_image_url")
    private String coverImageUrl;

    /**
     * Public URL of the trailer video.
     */
    @Column(name = "trailer_video_url")
    private String trailerVideoUrl;

    /**
     * Optional attachment file URL (e.g. PDF syllabus).
     */
    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}
