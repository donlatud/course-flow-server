package com.techup.course_flow_server.upload;

import org.springframework.web.multipart.MultipartFile;

/** Size and content-type checks for admin uploads. */
public final class UploadValidators {

    private static final long MAX_IMAGE_BYTES = 15L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;
    private static final long MAX_ATTACHMENT_BYTES = 50L * 1024 * 1024;

    private UploadValidators() {}

    public static void requireNonEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
    }

    public static void validateImage(MultipartFile file) {
        requireNonEmpty(file);
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed (e.g. JPEG, PNG, WebP)");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image is too large (max 15 MB)");
        }
    }

    public static void validateVideo(MultipartFile file) {
        requireNonEmpty(file);
        String ct = file.getContentType();
        if (ct == null
                || !(ct.startsWith("video/")
                        || "application/mp4".equalsIgnoreCase(ct))) {
            throw new IllegalArgumentException("Only video files are allowed (e.g. MP4, WebM)");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("Video is too large (max 500 MB)");
        }
    }

    public static void validateAttachment(MultipartFile file) {
        requireNonEmpty(file);
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("File is too large (max 50 MB)");
        }
    }
}
