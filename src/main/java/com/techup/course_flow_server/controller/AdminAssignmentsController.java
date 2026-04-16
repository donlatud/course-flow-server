package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.dto.assignment.AdminAssignmentResponse;
import com.techup.course_flow_server.dto.assignment.CreateAssignmentRequest;
import com.techup.course_flow_server.dto.assignment.UpdateAssignmentRequest;
import com.techup.course_flow_server.entity.Assignment;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.CourseModule;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.repository.AssignmentRepository;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/assignments")
public class AdminAssignmentsController {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MaterialRepository materialRepository;

    public AdminAssignmentsController(AssignmentRepository assignmentRepository,
                                      CourseRepository courseRepository,
                                      CourseModuleRepository courseModuleRepository,
                                      MaterialRepository materialRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.materialRepository = materialRepository;
    }

    /**
     * Get all assignments across all courses.
     * Supports sorting by: course, module, material, startDate, endDate, createdAt
     * Supports pagination: page, size
     * Supports search by: title (case-insensitive, contains)
     * Sorting is done in application layer to handle null values properly (NULLS LAST)
     */
    @GetMapping
    @Transactional(readOnly = true)
    public Page<AdminAssignmentResponse> getAllAssignments(
            Pageable pageable,
            @RequestParam(required = false) String search) {
        System.out.println("=== getAllAssignments START ===");
        System.out.println("Pageable: " + pageable);
        System.out.println("Search: " + search);
        
        // Fetch all assignments without sorting from database
        Pageable unsorted = org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            Sort.unsorted()
        );
        System.out.println("Unsorted pageable: " + unsorted);
        
        Page<Assignment> assignments;
        if (search != null && !search.isBlank()) {
            // Search by title (case-insensitive, contains)
            assignments = assignmentRepository.findByTitleContainingIgnoreCase(search.trim(), unsorted);
            System.out.println("Search applied: " + search.trim());
        } else {
            assignments = assignmentRepository.findAllWithPagination(unsorted);
        }
        System.out.println("Assignments fetched: " + assignments.getContent().size());
        System.out.println("Total elements: " + assignments.getTotalElements());
        
        // Convert to DTO
        List<AdminAssignmentResponse> responses = assignments.getContent().stream()
                .map(this::convertToAdminAssignmentResponse)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("Responses converted: " + responses.size());
        
        // Sort in application layer to handle nulls properly
        if (pageable.getSort().isSorted()) {
            System.out.println("Sorting enabled: " + pageable.getSort());
            java.util.Comparator<AdminAssignmentResponse> comparator = null;
            for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                System.out.println("Processing sort order: " + order.getProperty() + " " + order.getDirection());
                java.util.Comparator<AdminAssignmentResponse> orderComparator = createComparator(order);
                if (comparator == null) {
                    comparator = orderComparator;
                } else {
                    comparator = comparator.thenComparing(orderComparator);
                }
            }
            if (comparator != null) {
                responses.sort(comparator);
                System.out.println("Sorting completed");
            }
        }
        
        // Return as Page
        System.out.println("=== getAllAssignments END ===");
        return new org.springframework.data.domain.PageImpl<>(
            responses,
            pageable,
            assignments.getTotalElements()
        );
    }
    
    private java.util.Comparator<AdminAssignmentResponse> createComparator(org.springframework.data.domain.Sort.Order order) {
        java.util.Comparator<AdminAssignmentResponse> comparator = null;
        String property = order.getProperty();
        boolean ascending = order.isAscending();
        
        switch (property) {
            case "course.title":
                comparator = Comparator.comparing(
                    a -> a.getCourseTitle() != null ? a.getCourseTitle() : "",
                    Comparator.nullsLast(String::compareTo)
                );
                break;
            case "module.title":
                comparator = Comparator.comparing(
                    a -> a.getModuleTitle() != null ? a.getModuleTitle() : "",
                    Comparator.nullsLast(String::compareTo)
                );
                break;
            case "material.title":
                comparator = Comparator.comparing(
                    a -> a.getMaterialTitle() != null ? a.getMaterialTitle() : "",
                    Comparator.nullsLast(String::compareTo)
                );
                break;
            case "startDate":
                comparator = Comparator.comparing(
                    AdminAssignmentResponse::getStartDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "endDate":
                comparator = Comparator.comparing(
                    AdminAssignmentResponse::getEndDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            case "createdAt":
                comparator = Comparator.comparing(
                    AdminAssignmentResponse::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;
            default:
                // Unknown property, ignore
                break;
        }
        
        if (comparator != null && !ascending) {
            comparator = comparator.reversed();
        }
        
        return comparator;
    }

    /**
     * Get a single assignment by its ID.
     * Joins with course table to get course title.
     */
    @GetMapping("/{assignmentId}")
    public AdminAssignmentResponse getAssignmentById(@PathVariable("assignmentId") UUID assignmentId) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
        return convertToAdminAssignmentResponse(assignment);
    }

    /**
     * Create a new assignment.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAssignmentResponse createAssignment(@Valid @RequestBody CreateAssignmentRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()));

        // Get module and material if provided
        CourseModule module = null;
        Material material = null;

        if (request.getModuleId() != null) {
            module = courseModuleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new RuntimeException("Module not found with id: " + request.getModuleId()));
        }

        if (request.getMaterialId() != null) {
            material = materialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> new RuntimeException("Material not found with id: " + request.getMaterialId()));
        }

        Assignment assignment = Assignment.builder()
                .course(course)
                .module(module)
                .material(material)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToAdminAssignmentResponse(savedAssignment);
    }

    /**
     * Update an existing assignment.
     */
    @PutMapping("/{assignmentId}")
    @Transactional
    public AdminAssignmentResponse updateAssignment(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findWithCourseById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));

        // Update course if provided
        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()));
            assignment.setCourse(course);
        }

        // Update module if provided
        if (request.getModuleId() != null) {
            CourseModule module = courseModuleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new RuntimeException("Module not found with id: " + request.getModuleId()));
            assignment.setModule(module);
        }

        // Update material if provided
        if (request.getMaterialId() != null) {
            Material material = materialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> new RuntimeException("Material not found with id: " + request.getMaterialId()));
            assignment.setMaterial(material);
        }

        // Update other fields
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());

        Assignment updatedAssignment = assignmentRepository.save(assignment);
        return convertToAdminAssignmentResponse(updatedAssignment);
    }

    /**
     * Delete an assignment.
     */
    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable("assignmentId") UUID assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
        assignmentRepository.delete(assignment);
    }

    private AdminAssignmentResponse convertToAdminAssignmentResponse(Assignment assignment) {
        return AdminAssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null)
                .courseTitle(assignment.getCourse() != null ? assignment.getCourse().getTitle() : null)
                .moduleId(assignment.getModule() != null ? assignment.getModule().getId() : null)
                .moduleTitle(assignment.getModule() != null ? assignment.getModule().getTitle() : null)
                .materialId(assignment.getMaterial() != null ? assignment.getMaterial().getId() : null)
                .materialTitle(assignment.getMaterial() != null ? assignment.getMaterial().getTitle() : null)
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .createdAt(assignment.getCreatedAt())
                .build();
    }
}
