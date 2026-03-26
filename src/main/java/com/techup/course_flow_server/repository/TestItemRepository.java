package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.TestItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestItemRepository extends JpaRepository<TestItem, UUID> {
}
