package com.techup.course_flow_server.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import com.techup.course_flow_server.upload.cloudinary.CloudinaryUploadException;
import com.techup.course_flow_server.upload.supabase.SupabaseStorageException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldMessage)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        String msg = exception.getMostSpecificCause().getMessage();
        String message;
        if (msg != null && msg.contains("uk_courses_title")) {
            message = "A course with this title already exists";
        } else if (msg != null) {
            String oneLine = msg.replaceAll("\\s+", " ").trim();
            if (oneLine.length() > 300) {
                oneLine = oneLine.substring(0, 297) + "...";
            }
            message = oneLine;
        } else {
            message = "A record with the same value already exists";
        }
        return build(HttpStatus.CONFLICT, "DUPLICATE_ENTRY", message, request.getRequestURI());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = status == HttpStatus.FORBIDDEN ? "FORBIDDEN" : status.name();
        return build(status, code, exception.getReason(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_STATE", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(SupabaseStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleSupabaseStorage(
            SupabaseStorageException exception,
            HttpServletRequest request) {
        return build(
                HttpStatus.BAD_GATEWAY,
                "STORAGE_UPLOAD_FAILED",
                exception.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleCloudinaryUpload(
            CloudinaryUploadException exception,
            HttpServletRequest request) {
        return build(
                HttpStatus.BAD_GATEWAY,
                "VIDEO_UPLOAD_FAILED",
                exception.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        return build(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "Uploaded file exceeds the maximum allowed size",
                request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "Request body is invalid or malformed",
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        // Log the actual exception for debugging
        System.err.println("=== UNEXPECTED EXCEPTION ===");
        System.err.println("Path: " + request.getRequestURI());
        exception.printStackTrace();
        System.err.println("=== END EXCEPTION ===");
        
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            String path) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private String toFieldMessage(FieldError error) {
        String field = error.getField();
        String defaultMessage = error.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return field + " is invalid";
        }
        return field + ": " + defaultMessage;
    }
}
