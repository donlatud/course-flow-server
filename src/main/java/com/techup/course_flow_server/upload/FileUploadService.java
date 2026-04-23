package com.techup.course_flow_server.upload;

import com.techup.course_flow_server.config.SupabaseProperties;
import com.techup.course_flow_server.dto.upload.UploadUrlResponse;
import com.techup.course_flow_server.upload.supabase.SupabaseStorageClient;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    private final SupabaseStorageClient storageClient;
    private final SupabaseProperties supabaseProperties;

    public FileUploadService(SupabaseStorageClient storageClient, SupabaseProperties supabaseProperties) {
        this.storageClient = storageClient;
        this.supabaseProperties = supabaseProperties;
    }

    /** Course attachment → {@code course_file} bucket, {@code {id}/file/{original file name}}. */
    public UploadUrlResponse uploadAttachment(MultipartFile file, String courseFolderId) {
        UploadValidators.validateAttachment(file);
        String bucket = fileBucket();
        String objectPath =
                UploadPathUtils.buildPath(
                        courseFolderId, "file", UploadPathUtils.safeAttachmentFileName(file));
        String ct = file.getContentType();
        if (ct == null || ct.isBlank()) {
            ct = "application/octet-stream";
        }
        try {
            String url = storageClient.upload(bucket, objectPath, file.getBytes(), ct);
            return new UploadUrlResponse(url, "SUPABASE");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    private String fileBucket() {
        String b = supabaseProperties.bucketFile();
        return (b != null && !b.isBlank()) ? b : "course_file";
    }
}
