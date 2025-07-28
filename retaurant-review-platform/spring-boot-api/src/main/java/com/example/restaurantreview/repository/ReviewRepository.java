package com.example.restaurantreview.repository;

import com.example.restaurantreview.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurantId(Long restaurantId);

    Page<Review> findByRestaurantId(Long restaurantId, Pageable pageable);

    List<Review> findByRestaurantIdAndSentimentLabel(Long restaurantId, String sentimentLabel);

    @Query("SELECT r FROM Review r WHERE r.restaurant.id = :restaurantId AND r.createdAt >= :since")
    List<Review> findRecentReviewsForRestaurant(@Param("restaurantId") Long restaurantId,
                                                @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId AND r.isPositive = true")
    Long countPositiveReviewsForRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId AND r.isPositive = false")
    Long countNegativeReviewsForRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT AVG(r.sentimentScore) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double getAverageSentimentScoreForRestaurant(@Param("restaurantId") Long restaurantId);
}
