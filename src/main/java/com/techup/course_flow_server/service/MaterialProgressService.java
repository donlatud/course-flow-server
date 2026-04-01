package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.materialprogress.BatchUpdateItem;
import com.techup.course_flow_server.dto.materialprogress.BatchUpdateRequest;
import com.techup.course_flow_server.dto.materialprogress.BatchUpdateResponse;
import com.techup.course_flow_server.dto.materialprogress.BatchUpdateResultItem;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressCreateRequest;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressResponse;
import com.techup.course_flow_server.dto.materialprogress.MaterialProgressUpdateRequest;
import com.techup.course_flow_server.entity.CourseModule;
import com.techup.course_flow_server.entity.Enrollment;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.entity.MaterialProgress;
import com.techup.course_flow_server.entity.ModuleProgress;
import com.techup.course_flow_server.mapper.MaterialProgressMapper;
import com.techup.course_flow_server.repository.CourseModuleRepository;
import com.techup.course_flow_server.repository.EnrollmentRepository;
import com.techup.course_flow_server.repository.MaterialProgressRepository;
import com.techup.course_flow_server.repository.MaterialRepository;
import com.techup.course_flow_server.repository.ModuleProgressRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MaterialProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final MaterialRepository materialRepository;
    private final MaterialProgressRepository materialProgressRepository;
    private final ModuleProgressRepository moduleProgressRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MaterialProgressMapper materialProgressMapper;

    public MaterialProgressService(
            EnrollmentRepository enrollmentRepository,
            MaterialRepository materialRepository,
            MaterialProgressRepository materialProgressRepository,
            ModuleProgressRepository moduleProgressRepository,
            CourseModuleRepository courseModuleRepository,
            MaterialProgressMapper materialProgressMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.materialRepository = materialRepository;
        this.materialProgressRepository = materialProgressRepository;
        this.moduleProgressRepository = moduleProgressRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.materialProgressMapper = materialProgressMapper;
    }

    @Transactional
    public MaterialProgressResponse createProgress(MaterialProgressCreateRequest request, UUID userId) {
        Enrollment enrollment = getEnrollmentAndValidateOwner(request.getEnrollmentId(), userId);
        Material material = getMaterialAndValidateOwnership(request.getMaterialId(), enrollment);

        MaterialProgress progress = materialProgressRepository
                .findByEnrollmentIdAndMaterialId(enrollment.getId(), material.getId())
                .orElseGet(() -> MaterialProgress.builder()
                        .enrollment(enrollment)
                        .material(material)
                        .status(MaterialProgress.Status.NOT_STARTED)
                        .lastPosition(0)
                        .build());

        applyCreateDefaults(progress, request, material);
        MaterialProgress saved = materialProgressRepository.save(progress);

        boolean moduleCompleted = syncModuleProgress(enrollment, material.getModule());
        BigDecimal enrollmentProgressPercentage = recalculateEnrollmentProgress(enrollment);

        return toResponse(saved, moduleCompleted, enrollmentProgressPercentage);
    }

    @Transactional
    public BatchUpdateResponse batchUpdateProgress(BatchUpdateRequest batchRequest, UUID userId) {
        List<BatchUpdateResultItem> results = new ArrayList<>();

        for (BatchUpdateItem item : batchRequest.getUpdates()) {
            try {
                MaterialProgressUpdateRequest updateRequest = MaterialProgressUpdateRequest.builder()
                        .status(item.getStatus())
                        .lastPosition(item.getLastPosition())
                        .completedAt(item.getCompletedAt())
                        .build();

                MaterialProgressResponse response = updateProgress(item.getProgressId(), updateRequest, userId);
                
                results.add(BatchUpdateResultItem.builder()
                        .progressId(item.getProgressId())
                        .success(true)
                        .data(response)
                        .build());
            } catch (Exception e) {
                results.add(BatchUpdateResultItem.builder()
                        .progressId(item.getProgressId())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        int successCount = (int) results.stream().filter(BatchUpdateResultItem::isSuccess).count();
        int failureCount = results.size() - successCount;

        return BatchUpdateResponse.builder()
                .totalCount(results.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    @Transactional
    public MaterialProgressResponse updateProgress(UUID progressId, MaterialProgressUpdateRequest request, UUID userId) {
        MaterialProgress progress = materialProgressRepository.findById(progressId)
                .orElseThrow(() -> new IllegalArgumentException("Material progress not found"));

        if (!progress.getEnrollment().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This progress does not belong to current user");
        }

        // Idempotent completion: If already COMPLETED, only allow status/completedAt/lastPosition that maintains COMPLETED state
        if (progress.getStatus() == MaterialProgress.Status.COMPLETED) {
            // Return current state without modification if trying to downgrade from COMPLETED
            if (request.getStatus() != null && request.getStatus() != MaterialProgress.Status.COMPLETED) {
                boolean moduleCompleted = isModuleCompleted(progress.getEnrollment(), progress.getMaterial().getModule());
                BigDecimal enrollmentProgressPercentage = getEnrollmentProgressPercentage(progress.getEnrollment());
                return toResponse(progress, moduleCompleted, enrollmentProgressPercentage);
            }
            // Allow updating lastPosition even when COMPLETED (for resume functionality)
            if (request.getLastPosition() != null) {
                progress.setLastPosition(request.getLastPosition());
                MaterialProgress saved = materialProgressRepository.save(progress);
                boolean moduleCompleted = isModuleCompleted(saved.getEnrollment(), saved.getMaterial().getModule());
                BigDecimal enrollmentProgressPercentage = getEnrollmentProgressPercentage(saved.getEnrollment());
                return toResponse(saved, moduleCompleted, enrollmentProgressPercentage);
            }
            // Return current state without modification
            boolean moduleCompleted = isModuleCompleted(progress.getEnrollment(), progress.getMaterial().getModule());
            BigDecimal enrollmentProgressPercentage = getEnrollmentProgressPercentage(progress.getEnrollment());
            return toResponse(progress, moduleCompleted, enrollmentProgressPercentage);
        }

        materialProgressMapper.updateEntityFromRequest(request, progress);
        normalizeProgressForMaterialType(progress);
        MaterialProgress saved = materialProgressRepository.save(progress);

        boolean moduleCompleted = syncModuleProgress(saved.getEnrollment(), saved.getMaterial().getModule());
        BigDecimal enrollmentProgressPercentage = recalculateEnrollmentProgress(saved.getEnrollment());

        return toResponse(saved, moduleCompleted, enrollmentProgressPercentage);
    }

    private Enrollment getEnrollmentAndValidateOwner(UUID enrollmentId, UUID userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        if (!enrollment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This enrollment does not belong to current user");
        }
        return enrollment;
    }

    private Material getMaterialAndValidateOwnership(UUID materialId, Enrollment enrollment) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));
        UUID materialCourseId = material.getModule().getCourse().getId();
        UUID enrollmentCourseId = enrollment.getCourse().getId();
        if (!materialCourseId.equals(enrollmentCourseId)) {
            throw new IllegalArgumentException("Material is not in enrolled course");
        }
        return material;
    }

    private void applyCreateDefaults(MaterialProgress progress, MaterialProgressCreateRequest request, Material material) {
        if (request.getStatus() != null) {
            progress.setStatus(request.getStatus());
        } else if (progress.getStatus() == MaterialProgress.Status.NOT_STARTED) {
            progress.setStatus(MaterialProgress.Status.IN_PROGRESS);
        }

        if (request.getLastPosition() != null) {
            progress.setLastPosition(request.getLastPosition());
        }

        if (request.getCompletedAt() != null) {
            progress.setCompletedAt(request.getCompletedAt());
        }

        normalizeProgressForMaterialType(progress);
        if (progress.getStatus() == MaterialProgress.Status.COMPLETED && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        if (material.getFileType() != Material.FileType.VIDEO && progress.getLastPosition() == null) {
            progress.setLastPosition(0);
        }
    }

    private void normalizeProgressForMaterialType(MaterialProgress progress) {
        Material material = progress.getMaterial();
        if (material.getFileType() != Material.FileType.VIDEO && progress.getStatus() == MaterialProgress.Status.IN_PROGRESS) {
            progress.setStatus(MaterialProgress.Status.COMPLETED);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
            progress.setLastPosition(0);
        }
        if (progress.getStatus() == MaterialProgress.Status.COMPLETED && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
    }

    private boolean syncModuleProgress(Enrollment enrollment, CourseModule module) {
        boolean moduleCompleted = isModuleCompleted(enrollment, module);

        ModuleProgress moduleProgress = moduleProgressRepository
                .findByEnrollmentIdAndModuleId(enrollment.getId(), module.getId())
                .orElseGet(() -> ModuleProgress.builder().enrollment(enrollment).module(module).build());
        
        // Idempotent: only update completedAt if transitioning to completed
        boolean wasCompleted = Boolean.TRUE.equals(moduleProgress.getIsCompleted());
        moduleProgress.setIsCompleted(moduleCompleted);
        if (moduleCompleted && !wasCompleted) {
            moduleProgress.setCompletedAt(LocalDateTime.now());
        } else if (!moduleCompleted) {
            moduleProgress.setCompletedAt(null);
        }
        moduleProgressRepository.save(moduleProgress);

        return moduleCompleted;
    }

    private boolean isModuleCompleted(Enrollment enrollment, CourseModule module) {
        List<Material> moduleMaterials = materialRepository.findAllByModuleIdOrderByOrderIndexAsc(module.getId());
        if (moduleMaterials.isEmpty()) {
            return false;
        }

        Map<UUID, MaterialProgress> progressByMaterialId = materialProgressRepository
                .findAllByEnrollmentId(enrollment.getId())
                .stream()
                .collect(Collectors.toMap(mp -> mp.getMaterial().getId(), Function.identity()));

        return moduleMaterials.stream()
                .allMatch(material -> {
                    MaterialProgress progress = progressByMaterialId.get(material.getId());
                    return progress != null && progress.getStatus() == MaterialProgress.Status.COMPLETED;
                });
    }

    private BigDecimal getEnrollmentProgressPercentage(Enrollment enrollment) {
        return enrollment.getProgressPercentage() != null
                ? enrollment.getProgressPercentage()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal recalculateEnrollmentProgress(Enrollment enrollment) {
        List<CourseModule> courseModules = courseModuleRepository.findAllByCourseIdOrderByOrderIndexAsc(
                enrollment.getCourse().getId());
        List<UUID> moduleIds = courseModules.stream().map(CourseModule::getId).toList();

        List<Material> materials = moduleIds.isEmpty()
                ? List.of()
                : materialRepository.findAllByModuleIdInOrderByModuleOrderIndexAscOrderIndexAsc(moduleIds);
        int totalMaterials = materials.size();

        Map<UUID, MaterialProgress> progressByMaterialId = materialProgressRepository
                .findAllByEnrollmentId(enrollment.getId())
                .stream()
                .collect(Collectors.toMap(mp -> mp.getMaterial().getId(), Function.identity()));

        int completedMaterials = (int) materials.stream()
                .filter(material -> {
                    MaterialProgress progress = progressByMaterialId.get(material.getId());
                    return progress != null && progress.getStatus() == MaterialProgress.Status.COMPLETED;
                })
                .count();

        BigDecimal progressPercentage = toPercentage(completedMaterials, totalMaterials);
        enrollment.setProgressPercentage(progressPercentage);
        enrollmentRepository.save(enrollment);
        return progressPercentage;
    }

    private BigDecimal toPercentage(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private MaterialProgressResponse toResponse(
            MaterialProgress progress,
            boolean moduleCompleted,
            BigDecimal enrollmentProgressPercentage) {
        return MaterialProgressResponse.builder()
                .id(progress.getId())
                .enrollmentId(progress.getEnrollment().getId())
                .materialId(progress.getMaterial().getId())
                .status(progress.getStatus())
                .lastPosition(progress.getLastPosition())
                .completedAt(progress.getCompletedAt())
                .updatedAt(progress.getUpdatedAt())
                .moduleCompleted(moduleCompleted)
                .enrollmentProgressPercentage(enrollmentProgressPercentage)
                .build();
    }
}
