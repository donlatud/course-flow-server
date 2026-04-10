package com.techup.course_flow_server.upload;

import com.techup.course_flow_server.dto.upload.UploadUrlResponse;
import com.techup.course_flow_server.upload.cloudinary.CloudinaryClient;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoUploadService {

    private final CloudinaryClient cloudinaryClient;

    public VideoUploadService(CloudinaryClient cloudinaryClient) {
        this.cloudinaryClient = cloudinaryClient;
    }

    /**
     * @param kind {@code TRAILER} or {@code SUBLESSON}; sub-lesson requires lessonId and subLessonId.
     */
    public UploadUrlResponse upload(MultipartFile file, String courseFolderId, String kind, Integer lessonId, Integer subLessonId) {
        UploadValidators.validateVideo(file);
        String folder;
        if ("TRAILER".equalsIgnoreCase(kind)) {
            folder = "courses/" + courseFolderId.trim() + "/trailer";
        } else if ("SUBLESSON".equalsIgnoreCase(kind)) {
            if (lessonId == null || subLessonId == null) {
                throw new IllegalArgumentException("lessonId and subLessonId are required for SUBLESSON video upload");
            }
            folder =
                    "courses/"
                            + courseFolderId.trim()
                            + "/lesson/"
                            + lessonId
                            + "/sublesson/"
                            + subLessonId;
        } else {
            throw new IllegalArgumentException("kind must be TRAILER or SUBLESSON");
        }

        String ext = UploadPathUtils.fileExtension(file, "mp4");
        String filename = "video-" + UUID.randomUUID() + "." + ext;

        try {
            String ct = file.getContentType();
            if (ct == null || ct.isBlank()) {
                ct = "video/mp4";
            }
            String url =
                    cloudinaryClient.uploadVideo(
                            file.getBytes(), folder, filename, ct);
            return UploadUrlResponse.builder().url(url).provider("CLOUDINARY").build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }
}
