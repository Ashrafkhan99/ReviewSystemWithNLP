package com.example.restaurantreview.controller;

import com.example.restaurantreview.dto.ReviewRequest;
import com.example.restaurantreview.entity.Review;
import com.example.restaurantreview.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> createReview(@Valid @RequestBody ReviewRequest request) {
        try {
            logger.info("Received review submission for restaurant ID: {}", request.getRestaurantId());
            Review review = reviewService.createReview(request);

            Map<String, Object> response = Map.of(
                    "review", review,
                    "message", "Review created successfully and sentiment analyzed",
                    "sentiment", review.getSentimentLabel(),
                    "sentimentScore", review.getSentimentScore()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error creating review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<Page<Review>> getReviewsByRestaurant(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Review> reviews = reviewService.getReviewsByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/restaurant/{restaurantId}/sentiment/{sentiment}")
    public ResponseEntity<List<Review>> getReviewsBySentiment(
            @PathVariable Long restaurantId,
            @PathVariable String sentiment) {

        List<Review> reviews = reviewService.getReviewsBySentiment(restaurantId, sentiment.toUpperCase());
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/restaurant/{restaurantId}/recent")
    public ResponseEntity<List<Review>> getRecentReviews(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "24") int hours) {

        List<Review> reviews = reviewService.getRecentReviews(restaurantId, hours);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/restaurant/{restaurantId}/statistics")
    public ResponseEntity<ReviewService.ReviewStatistics> getReviewStatistics(@PathVariable Long restaurantId) {
        ReviewService.ReviewStatistics stats = reviewService.getReviewStatistics(restaurantId);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
        } catch (RuntimeException e) {
            logger.error("Error deleting review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
