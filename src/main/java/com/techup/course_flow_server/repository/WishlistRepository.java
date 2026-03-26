package com.techup.course_flow_server.repository;

import com.techup.course_flow_server.entity.Wishlist;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
}
