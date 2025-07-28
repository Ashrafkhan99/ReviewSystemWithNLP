package com.example.restaurantreview.repository;

import com.example.restaurantreview.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByNameIgnoreCase(String name);

    List<Restaurant> findByCuisineTypeIgnoreCase(String cuisineType);

    Page<Restaurant> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE r.averageScore >= :minScore ORDER BY r.averageScore DESC")
    List<Restaurant> findByMinimumScore(@Param("minScore") Double minScore);

    @Query("SELECT r FROM Restaurant r ORDER BY r.averageScore DESC")
    List<Restaurant> findAllOrderByAverageScoreDesc();

    @Query("SELECT r FROM Restaurant r ORDER BY r.reviewCount DESC")
    List<Restaurant> findAllOrderByReviewCountDesc();
}
