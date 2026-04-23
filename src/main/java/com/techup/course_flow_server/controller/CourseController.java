package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.course.CourseDetailResponse;
import com.techup.course_flow_server.dto.course.CourseResponse;
import com.techup.course_flow_server.dto.course.CourseWithLessonCountResponse;
import com.techup.course_flow_server.service.CourseService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public CourseDetailResponse getCourseById(@PathVariable("id") UUID id) {
        return courseService.getCourseWithModulesAndMaterialsById(id);
    }

    @GetMapping("/published")
    public List<CourseWithLessonCountResponse> getPublishedCourses() {
        return courseService.getPublishedCourses();
    }

    @GetMapping("/category/{category}")
    public List<CourseResponse> getCoursesByCategory(@PathVariable("category") String category) {
        return courseService.getCoursesByCategory(category);
    }

    @GetMapping("/search")
    public List<CourseResponse> searchCourses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subject) {
        if (category != null && subject != null) {
            return courseService.getCoursesByCategory(category);
        }
        if (category != null) {
            return courseService.getCoursesByCategory(category);
        }
        return courseService.getAllCourses(); // Return all courses for search fallback
    }
}
