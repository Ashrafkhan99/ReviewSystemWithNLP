package com.example.restaurantreview.service;

import com.example.restaurantreview.dto.ReviewRequest;
import com.example.restaurantreview.dto.SentimentAnalysisResponse;
import com.example.restaurantreview.entity.Restaurant;
import com.example.restaurantreview.entity.Review;
import com.example.restaurantreview.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private NLPService nlpService;

    public Review createReview(ReviewRequest request) {
        logger.info("Creating new review for restaurant ID: {}", request.getRestaurantId());

        // Validate restaurant existence
        Restaurant restaurant = restaurantService.getRestaurantById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found with ID: " + request.getRestaurantId()));

        // Perform sentiment analysis
        SentimentAnalysisResponse sentimentResult = nlpService.analyzeSentiment(request.getReviewText());

        // Create review entity
        Review review = new Review(restaurant, request.getReviewText(), request.getReviewerName());
        review.setSentimentLabel(sentimentResult.getSentiment());
        review.setSentimentScore(sentimentResult.getScore());
        review.setSentimentConfidence(sentimentResult.getConfidence());
        review.setIsPositive(sentimentResult.getIsPositive());

        // Save review
        Review savedReview = reviewRepository.save(review);

        // Update restaurant scores (this will also update the leaderboard)
        restaurantService.updateRestaurantScores(restaurant, sentimentResult.getScore());

        logger.info("Successfully created review with ID: {} - Sentiment: {} ({})",
                savedReview.getId(), sentimentResult.getSentiment(), sentimentResult.getScore());

        return savedReview;
    }

    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public List<Review> getReviewsByRestaurant(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId);
    }

    public Page<Review> getReviewsByRestaurant(Long restaurantId, Pageable pageable) {
        return reviewRepository.findByRestaurantId(restaurantId, pageable);
    }

    public List<Review> getReviewsBySentiment(Long restaurantId, String sentiment) {
        return reviewRepository.findByRestaurantIdAndSentimentLabel(restaurantId, sentiment);
    }

    public List<Review> getRecentReviews(Long restaurantId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return reviewRepository.findRecentReviewsForRestaurant(restaurantId, since);
    }

    public ReviewStatistics getReviewStatistics(Long restaurantId) {
        Long positiveCount = reviewRepository.countPositiveReviewsForRestaurant(restaurantId);
        Long negativeCount = reviewRepository.countNegativeReviewsForRestaurant(restaurantId);
        Double averageSentiment = reviewRepository.getAverageSentimentScoreForRestaurant(restaurantId);

        return new ReviewStatistics(
                positiveCount != null ? positiveCount : 0L,
                negativeCount != null ? negativeCount : 0L,
                averageSentiment != null ? averageSentiment : 0.0
        );
    }

    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));

        logger.info("Deleting review with ID: {}", id);

        Restaurant restaurant = review.getRestaurant();

        // Adjust restaurant scores based on review deletion
        if (restaurant.getReviewCount() > 1) {
            double newTotalScore = restaurant.getTotalScore() - review.getSentimentScore();
            restaurant.setTotalScore(newTotalScore);
            restaurant.setReviewCount(restaurant.getReviewCount() - 1);
            restaurant.setAverageScore(newTotalScore / restaurant.getReviewCount());
        } else {
            // Reset scores if this was the only review
            restaurant.setTotalScore(0.0);
            restaurant.setReviewCount(0);
            restaurant.setAverageScore(0.0);
        }

        // Update restaurant and leaderboard
        restaurantService.updateRestaurantScores(restaurant, 0.0); // This will recalculate properly

        reviewRepository.delete(review);
    }

    // Inner class for review statistics
    public static class ReviewStatistics {
        private final Long positiveCount;
        private final Long negativeCount;
        private final Double averageSentiment;
        private final Long totalCount;
        private final Double positivePercentage;

        public ReviewStatistics(Long positiveCount, Long negativeCount, Double averageSentiment) {
            this.positiveCount = positiveCount;
            this.negativeCount = negativeCount;
            this.averageSentiment = averageSentiment;
            this.totalCount = positiveCount + negativeCount;
            this.positivePercentage = totalCount > 0 ? (positiveCount.doubleValue() / totalCount) * 100 : 0.0;
        }

        // Getters
        public Long getPositiveCount() { return positiveCount; }
        public Long getNegativeCount() { return negativeCount; }
        public Double getAverageSentiment() { return averageSentiment; }
        public Long getTotalCount() { return totalCount; }
        public Double getPositivePercentage() { return positivePercentage; }
    }
}
