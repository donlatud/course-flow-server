package com.techup.course_flow_server.upload;

import java.util.Arrays;
import org.springframework.web.multipart.MultipartFile;
import java.util.stream.Collectors;

public final class UploadPathUtils {

    private UploadPathUtils() {}

    public static String buildPath(String courseFolderId, String... segments) {
        if (courseFolderId == null || courseFolderId.isBlank()) {
            throw new IllegalArgumentException("courseFolderId is required");
        }
        String base = courseFolderId.trim();
        if (segments.length == 0) {
            return base;
        }
        return base
                + "/"
                + Arrays.stream(segments).map(String::trim).collect(Collectors.joining("/"));
    }

    public static String fileExtension(MultipartFile file, String fallback) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return fallback;
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        return ext.isBlank() ? fallback : ext;
    }

    /**
     * Uses the client-provided filename for storage (no random rename). Strips any path segments,
     * removes "..", and truncates very long names so the object key stays safe for Storage URLs.
     */
    public static String safeAttachmentFileName(MultipartFile file) {
        String ext = fileExtension(file, "bin");
        String raw = file.getOriginalFilename();
        if (raw == null || raw.isBlank()) {
            return "attachment." + ext;
        }
        String base = raw.trim();
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1).trim();
        }
        if (base.isEmpty()) {
            return "attachment." + ext;
        }
        base = base.replace("..", "_");
        // Storage / URL practicality
        final int max = 200;
        if (base.length() > max) {
            if (base.contains(".")) {
                String shortExt = base.substring(base.lastIndexOf('.') + 1);
                String stem = base.substring(0, base.lastIndexOf('.'));
                int budget = max - shortExt.length() - 1;
                if (budget > 8) {
                    base = stem.substring(0, Math.min(stem.length(), budget)) + "." + shortExt;
                } else {
                    base = "attachment." + ext;
                }
            } else {
                base = base.substring(0, max);
            }
        }
        return base;
    }
}
