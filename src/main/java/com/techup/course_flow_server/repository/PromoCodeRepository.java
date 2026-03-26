package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.PromoCode;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {
}
