package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
}
