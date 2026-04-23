package com.techup.course_flow_server.dto.upload;

/** JSON body for admin upload endpoints. Plain record (no Lombok) for reliable Jackson serialization. */
public record UploadUrlResponse(String url, String provider) {}
