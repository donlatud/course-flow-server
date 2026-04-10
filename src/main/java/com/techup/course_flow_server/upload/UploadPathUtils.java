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
}
