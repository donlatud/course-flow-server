package com.techup.course_flow_server.upload.supabase;

/** Thrown when Supabase Storage REST returns an error response. */
public class SupabaseStorageException extends RuntimeException {

    public SupabaseStorageException(String message) {
        super(message);
    }

    public SupabaseStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
