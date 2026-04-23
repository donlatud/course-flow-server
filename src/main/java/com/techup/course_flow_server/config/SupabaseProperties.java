package com.techup.course_flow_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
        String url,
        String bucket,
        String apiKey,
        /** Service-role key for server-side Storage uploads (bypasses RLS). */
        String serviceKey,
        /** Bucket for images (cover, sub-lesson). Default: course_image */
        String bucketImage,
        /** Bucket for attachments (PDF, etc.). Default: course_file */
        String bucketFile
) {
}
