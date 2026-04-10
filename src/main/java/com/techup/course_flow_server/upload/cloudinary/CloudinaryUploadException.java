package com.techup.course_flow_server.upload.cloudinary;

/** Thrown when Cloudinary upload API returns an error or invalid response. */
public class CloudinaryUploadException extends RuntimeException {

    public CloudinaryUploadException(String message) {
        super(message);
    }

    public CloudinaryUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
