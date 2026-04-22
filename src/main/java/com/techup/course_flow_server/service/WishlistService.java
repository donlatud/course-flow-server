package com.techup.course_flow_server.service;

import com.techup.course_flow_server.dto.wishlist.WishlistResponse;
import com.techup.course_flow_server.entity.Course;
import com.techup.course_flow_server.entity.User;
import com.techup.course_flow_server.entity.Wishlist;
import com.techup.course_flow_server.repository.CourseRepository;
import com.techup.course_flow_server.repository.UserRepository;
import com.techup.course_flow_server.repository.WishlistRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            UserRepository userRepository,
            CourseRepository courseRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public List<WishlistResponse> getMyWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId)
            .stream()
            .map(w -> new WishlistResponse(
                w.getId(),
                w.getCourse().getId(),
                w.getCourse().getTitle(),
                w.getCourse().getDescription(),
                w.getCourse().getCoverImageUrl(),
                w.getCourse().getPrice(),
                w.getCourse().getTotalLearningTime(),
                w.getAddedAt()
            ))
            .toList();
    }

    public boolean isInWishlist(UUID userId, UUID courseId) {
        return wishlistRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Transactional
    public WishlistResponse addToWishlist(UUID userId, UUID courseId) {
        if (wishlistRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already in wishlist");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Wishlist wishlist = Wishlist.builder()
            .user(user)
            .course(course)
            .build();

        wishlist = wishlistRepository.save(wishlist);

        return new WishlistResponse(
            wishlist.getId(),
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getCoverImageUrl(),
            course.getPrice(),
            course.getTotalLearningTime(),
            wishlist.getAddedAt()
        );
    }

    @Transactional
    public void removeFromWishlist(UUID userId, UUID courseId) {
        if (!wishlistRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not in wishlist");
        }
        wishlistRepository.deleteByUserIdAndCourseId(userId, courseId);
    }
}