package com.example.restaurantreview.service;

import com.example.restaurantreview.entity.Restaurant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LeaderboardService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.leaderboard.cache-key}")
    private String leaderboardKey;

    @Value("${app.leaderboard.top-limit:50}")
    private int topLimit;

    public void updateRestaurantScore(Restaurant restaurant) {
        try {
            String restaurantKey = "restaurant:" + restaurant.getId();
            Double score = restaurant.getAverageScore();

            logger.info("Updating leaderboard for restaurant {} with score {}", restaurant.getName(), score);

            // Create restaurant data for leaderboard
            RestaurantLeaderboardEntry entry = new RestaurantLeaderboardEntry(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAverageScore(),
                    restaurant.getReviewCount(),
                    restaurant.getCuisineType()
            );

            // Store detailed restaurant info
            redisTemplate.opsForHash().put(restaurantKey, "data", entry);

            // Update sorted set for leaderboard
            redisTemplate.opsForZSet().add(leaderboardKey, restaurantKey, score);

            logger.info("Successfully updated leaderboard for restaurant {}", restaurant.getName());

        } catch (Exception e) {
            logger.error("Error updating restaurant score in leaderboard: {}", e.getMessage());
        }
    }

    public List<RestaurantLeaderboardEntry> getTopRestaurants(int limit) {
        try {
            limit = Math.min(limit, topLimit);

            Set<ZSetOperations.TypedTuple<Object>> rankings = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(leaderboardKey, 0, limit - 1);

            List<RestaurantLeaderboardEntry> leaderboard = new ArrayList<>();

            if (rankings != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : rankings) {
                    String restaurantKey = (String) tuple.getValue();
                    Object data = redisTemplate.opsForHash().get(restaurantKey, "data");

                    if (data instanceof RestaurantLeaderboardEntry) {
                        RestaurantLeaderboardEntry entry = (RestaurantLeaderboardEntry) data;
                        entry.setRank(leaderboard.size() + 1);
                        leaderboard.add(entry);
                    }
                }
            }

            logger.info("Retrieved {} restaurants from leaderboard", leaderboard.size());
            return leaderboard;

        } catch (Exception e) {
            logger.error("Error retrieving leaderboard: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Long getRestaurantRank(Long restaurantId) {
        try {
            String restaurantKey = "restaurant:" + restaurantId;
            Long rank = redisTemplate.opsForZSet().reverseRank(leaderboardKey, restaurantKey);
            return rank != null ? rank + 1 : null; // Convert to 1-based ranking
        } catch (Exception e) {
            logger.error("Error getting restaurant rank: {}", e.getMessage());
            return null;
        }
    }

    public void removeRestaurantFromLeaderboard(Long restaurantId) {
        try {
            String restaurantKey = "restaurant:" + restaurantId;
            redisTemplate.opsForZSet().remove(leaderboardKey, restaurantKey);
            redisTemplate.delete(restaurantKey);
            logger.info("Removed restaurant {} from leaderboard", restaurantId);
        } catch (Exception e) {
            logger.error("Error removing restaurant from leaderboard: {}", e.getMessage());
        }
    }

    // Inner class for leaderboard entries
    public static class RestaurantLeaderboardEntry {
        private Long id;
        private String name;
        private Double averageScore;
        private Integer reviewCount;
        private String cuisineType;
        private Integer rank;

        public RestaurantLeaderboardEntry() {}

        public RestaurantLeaderboardEntry(Long id, String name, Double averageScore,
                                          Integer reviewCount, String cuisineType) {
            this.id = id;
            this.name = name;
            this.averageScore = averageScore;
            this.reviewCount = reviewCount;
            this.cuisineType = cuisineType;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

        public Integer getReviewCount() { return reviewCount; }
        public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

        public String getCuisineType() { return cuisineType; }
        public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }

        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
    }
}
