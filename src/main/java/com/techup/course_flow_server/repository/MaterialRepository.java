package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Material;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
}
