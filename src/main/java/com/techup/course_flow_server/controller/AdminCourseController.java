package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.admin.course.CourseAdminDetailResponse;
import com.techup.course_flow_server.dto.admin.course.CourseAdminSummaryResponse;
import com.techup.course_flow_server.dto.admin.course.CreateCourseRequest;
import com.techup.course_flow_server.dto.admin.course.UpdateCourseRequest;
import com.techup.course_flow_server.service.AdminCourseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    /**
     * GET /api/admin/courses
     * Returns the list of all courses for the admin course table.
     */
    @GetMapping
    public List<CourseAdminSummaryResponse> listCourses() {
        return adminCourseService.listCourses();
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

    /**
     * GET /api/admin/courses/{courseId}
     * Returns full course detail including all modules and sub-lessons.
     */
    @GetMapping("/{courseId}")
    public CourseAdminDetailResponse getCourse(@PathVariable UUID courseId) {
        return adminCourseService.getCourse(courseId);
    }

    /**
     * POST /api/admin/courses
     * Creates a new course with modules (lessons) and materials (sub-lessons) in one request.
     *
     * Request body matches the UI CourseCreatePage form state:
     * - title            → Course name
     * - description      → Course summary
     * - detail           → Course detail
     * - price            → Price (must be >= 0)
     * - totalLearningTime → Total learning time in hours (must be >= 1)
     * - promoCode        → Optional block, only sent when promoEnabled = true in UI
     * - modules          → List of lessons, each containing sub-lessons
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseAdminDetailResponse createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @RequestAttribute("authenticatedUserId") UUID adminUserId) {
        return adminCourseService.createCourse(request, adminUserId);
    }

    /**
     * PUT /api/admin/courses/{courseId}
     * Updates an existing course (replaces all modules + materials).
     */
    @PutMapping("/{courseId}")
    public CourseAdminDetailResponse updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            @RequestAttribute("authenticatedUserId") UUID adminUserId) {
        return adminCourseService.updateCourse(courseId, request, adminUserId);
    }

    /**
     * DELETE /api/admin/courses/{courseId}
     * Deletes a course along with all its modules and materials.
     */
    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @PathVariable UUID courseId,
            @RequestAttribute("authenticatedUserId") UUID adminUserId) {
        adminCourseService.deleteCourse(courseId, adminUserId);
    }
}
