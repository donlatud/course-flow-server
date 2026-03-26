package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.ModuleProgress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, UUID> {
}
