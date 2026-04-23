package com.techup.course_flow_server.mapper;

import com.techup.course_flow_server.dto.materialprogress.MaterialProgressUpdateRequest;
import com.techup.course_flow_server.entity.MaterialProgress;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructCentralConfig.class)
public interface MaterialProgressMapper {

    void updateEntityFromRequest(
            MaterialProgressUpdateRequest request,
            @MappingTarget MaterialProgress materialProgress);
}
