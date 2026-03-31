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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "materials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    private String title;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    /** Playback length in seconds; null for non-video materials. */
    private Integer duration;

    public enum FileType {
        PDF,
        IMAGE,
        VIDEO
    }
}
