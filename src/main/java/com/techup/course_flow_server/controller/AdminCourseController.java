package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminSummaryResponse;
import com.techup.course_flow_server.dto.admin.course.CreateCourseRequest;
import com.techup.course_flow_server.dto.admin.course.UpdateCourseRequest;
import com.techup.course_flow_server.service.AdminCourseService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    /**
     * GET /api/admin/courses?page=0&size=10&search=
     * Paginated list for the admin course table. {@code page} is 0-based.
     * Optional {@code search} filters by course title (case-insensitive, contains).
     * Optional {@code sortBy}: title, status, price, createdAt, updatedAt, lessonCount (default createdAt).
     * Optional {@code sortDir}: asc or desc (default desc).
     */
    @GetMapping
    public Page<CourseAdminSummaryResponse> listCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        return adminCourseService.listCourses(page, size, search, sortBy, sortDir);
    }

    /**
     * GET /api/admin/courses/exists?title=...
     * Checks whether a course with the given title already exists (case-insensitive).
     * Used by the frontend to validate uniqueness before submitting the create form.
     */
    @GetMapping("/exists")
    public Map<String, Boolean> checkTitleExists(@RequestParam String title) {
        return Map.of("exists", adminCourseService.isTitleTaken(title));
    }

    @GetMapping("/{courseId}")
    public CourseAdminDetailResponse getCourse(@PathVariable UUID courseId) {
        return adminCourseService.getCourse(courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseAdminDetailResponse createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return adminCourseService.createCourse(request, adminUserId);
    }

    @PutMapping("/{courseId}")
    public CourseAdminDetailResponse updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        return adminCourseService.updateCourse(courseId, request, adminUserId);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminUserId = UUID.fromString(jwt.getSubject());
        adminCourseService.deleteCourse(courseId, adminUserId);
    }
}