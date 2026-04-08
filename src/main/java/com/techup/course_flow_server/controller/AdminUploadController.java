package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.upload.UploadUrlResponse;
import com.techup.course_flow_server.security.MockAuthFilter;
import com.techup.course_flow_server.upload.FileUploadService;
import com.techup.course_flow_server.upload.ImageUploadService;
import com.techup.course_flow_server.upload.VideoUploadService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {

    private final ImageUploadService imageUploadService;
    private final VideoUploadService videoUploadService;
    private final FileUploadService fileUploadService;

    public AdminUploadController(
            ImageUploadService imageUploadService,
            VideoUploadService videoUploadService,
            FileUploadService fileUploadService) {
        this.imageUploadService = imageUploadService;
        this.videoUploadService = videoUploadService;
        this.fileUploadService = fileUploadService;
    }

    /**
     * Multipart: {@code file}, {@code courseFolderId}, {@code kind}=COVER|SUBLESSON, optional {@code lessonId},
     * {@code subLessonId} for SUBLESSON.
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadUrlResponse uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam("courseFolderId") String courseFolderId,
            @RequestParam(value = "kind", defaultValue = "COVER") String kind,
            @RequestParam(value = "lessonId", required = false) Integer lessonId,
            @RequestParam(value = "subLessonId", required = false) Integer subLessonId,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID adminUserId) {
        Objects.requireNonNull(adminUserId, "authenticated admin");
        return imageUploadService.upload(file, courseFolderId, kind, lessonId, subLessonId);
    }

    /**
     * Multipart: {@code file}, {@code courseFolderId}, {@code kind}=TRAILER|SUBLESSON, optional {@code lessonId},
     * {@code subLessonId} for SUBLESSON.
     */
    @PostMapping(value = "/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadUrlResponse uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestParam("courseFolderId") String courseFolderId,
            @RequestParam(value = "kind", defaultValue = "TRAILER") String kind,
            @RequestParam(value = "lessonId", required = false) Integer lessonId,
            @RequestParam(value = "subLessonId", required = false) Integer subLessonId,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID adminUserId) {
        Objects.requireNonNull(adminUserId, "authenticated admin");
        return videoUploadService.upload(file, courseFolderId, kind, lessonId, subLessonId);
    }

    /** Multipart: {@code file}, {@code courseFolderId}. */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadUrlResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("courseFolderId") String courseFolderId,
            @RequestAttribute(MockAuthFilter.AUTHENTICATED_USER_ID_ATTR) UUID adminUserId) {
        Objects.requireNonNull(adminUserId, "authenticated admin");
        return fileUploadService.uploadAttachment(file, courseFolderId);
    }
}
