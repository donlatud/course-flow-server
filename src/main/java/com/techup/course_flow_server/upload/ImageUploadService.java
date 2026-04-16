package com.techup.course_flow_server.upload;

import com.techup.course_flow_server.config.SupabaseProperties;
import com.techup.course_flow_server.dto.upload.UploadUrlResponse;
import com.techup.course_flow_server.upload.supabase.SupabaseStorageClient;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadService {

    private final SupabaseStorageClient storageClient;
    private final SupabaseProperties supabaseProperties;

    public ImageUploadService(SupabaseStorageClient storageClient, SupabaseProperties supabaseProperties) {
        this.storageClient = storageClient;
        this.supabaseProperties = supabaseProperties;
    }

    /**
     * @param kind {@code COVER} or {@code SUBLESSON}; sub-lesson requires lessonId and subLessonId.
     */
    public UploadUrlResponse upload(MultipartFile file, String courseFolderId, String kind, Integer lessonId, Integer subLessonId) {
        UploadValidators.validateImage(file);
        String bucket = imageBucket();
        String ext = UploadPathUtils.fileExtension(file, "jpg");
        String objectPath;
        if ("COVER".equalsIgnoreCase(kind)) {
            objectPath =
                    UploadPathUtils.buildPath(
                            courseFolderId, "cover_image", UUID.randomUUID() + "." + ext);
        } else if ("SUBLESSON".equalsIgnoreCase(kind)) {
            if (lessonId == null || subLessonId == null) {
                throw new IllegalArgumentException("lessonId and subLessonId are required for SUBLESSON image upload");
            }
            objectPath =
                    UploadPathUtils.buildPath(
                            courseFolderId,
                            "lesson",
                            String.valueOf(lessonId),
                            "sublesson",
                            String.valueOf(subLessonId),
                            UUID.randomUUID() + "." + ext);
        } else {
            throw new IllegalArgumentException("kind must be COVER or SUBLESSON");
        }

        try {
            String url =
                    storageClient.upload(
                            bucket,
                            objectPath,
                            file.getBytes(),
                            file.getContentType() != null ? file.getContentType() : "image/jpeg");
            return new UploadUrlResponse(url, "SUPABASE");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    private String imageBucket() {
        String b = supabaseProperties.bucketImage();
        return (b != null && !b.isBlank()) ? b : "course_image";
    }
}
