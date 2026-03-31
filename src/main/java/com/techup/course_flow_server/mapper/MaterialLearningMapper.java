package com.techup.course_flow_server.mapper;

import com.techup.course_flow_server.dto.courselearning.MaterialLearningResponse;
import com.techup.course_flow_server.entity.Material;
import com.techup.course_flow_server.entity.MaterialProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructCentralConfig.class)
public interface MaterialLearningMapper {

    @Mapping(target = "materialId", source = "material.id")
    @Mapping(target = "title", source = "material.title")
    @Mapping(target = "orderIndex", source = "material.orderIndex")
    @Mapping(target = "fileUrl", source = "material.fileUrl")
    @Mapping(target = "fileType", source = "material.fileType")
    @Mapping(target = "duration", source = "material.duration")
    @Mapping(
            target = "status",
            expression =
                    "java(progress != null ? progress.getStatus() : com.techup.course_flow_server.entity.MaterialProgress.Status.NOT_STARTED)")
    @Mapping(target = "lastPosition", expression = "java(progress != null ? progress.getLastPosition() : 0)")
    @Mapping(
            target = "completed",
            expression =
                    "java(progress != null && progress.getStatus() == com.techup.course_flow_server.entity.MaterialProgress.Status.COMPLETED)")
    MaterialLearningResponse toResponse(Material material, MaterialProgress progress);
}
