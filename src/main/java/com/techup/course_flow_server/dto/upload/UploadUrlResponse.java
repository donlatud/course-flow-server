package com.techup.course_flow_server.dto.upload;

import lombok.Builder;

@Builder
public record UploadUrlResponse(String url, String provider) {}
