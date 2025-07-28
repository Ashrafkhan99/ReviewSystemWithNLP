package com.example.restaurantreview.service;

import com.example.restaurantreview.dto.RestaurantRequest;
import com.example.restaurantreview.entity.Restaurant;
import com.example.restaurantreview.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RestaurantService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantService.class);

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private LeaderboardService leaderboardService;

    public Restaurant createRestaurant(RestaurantRequest request) {
        logger.info("Creating new restaurant: {}", request.getName());

        // Check if a restaurant with the same name already exists
        Optional<Restaurant> existing = restaurantRepository.findByNameIgnoreCase(request.getName());
        if (existing.isPresent()) {
            throw new RuntimeException("Restaurant with name '" + request.getName() + "' already exists");
        }

        Restaurant restaurant = new Restaurant(
                request.getName(),
                request.getDescription(),
                request.getAddress(),
                request.getCuisineType()
        );

        Restaurant saved = restaurantRepository.save(restaurant);

        // Initialize in leaderboard with zero score
        leaderboardService.updateRestaurantScore(saved);

        logger.info("Successfully created restaurant with ID: {}", saved.getId());
        return saved;
    }

    public Optional<Restaurant> getRestaurantById(Long id) {
        return restaurantRepository.findById(id);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public Page<Restaurant> searchRestaurants(String name, Pageable pageable) {
        if (name == null || name.trim().isEmpty()) {
            return restaurantRepository.findAll(pageable);
        }
        return restaurantRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
    }

    public List<Restaurant> getRestaurantsByCuisine(String cuisineType) {
        return restaurantRepository.findByCuisineTypeIgnoreCase(cuisineType);
    }

    public Restaurant updateRestaurant(Long id, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with ID: " + id));

        logger.info("Updating restaurant: {}", restaurant.getName());

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCuisineType(request.getCuisineType());

        Restaurant updated = restaurantRepository.save(restaurant);

        // Update leaderboard with new information
        leaderboardService.updateRestaurantScore(updated);

        return updated;
    }

    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with ID: " + id));

        logger.info("Deleting restaurant: {}", restaurant.getName());

        // Remove from leaderboard
        leaderboardService.removeRestaurantFromLeaderboard(id);

        restaurantRepository.delete(restaurant);
    }

    public void updateRestaurantScores(Restaurant restaurant, double newSentimentScore) {
        logger.info("Updating scores for restaurant: {}", restaurant.getName());

        // Update total score and count
        restaurant.setTotalScore(restaurant.getTotalScore() + newSentimentScore);
        restaurant.setReviewCount(restaurant.getReviewCount() + 1);

        // Recalculate average score
        double averageScore = restaurant.getTotalScore() / restaurant.getReviewCount();
        restaurant.setAverageScore(averageScore);

        // Save to database
        Restaurant updated = restaurantRepository.save(restaurant);

        // Update leaderboard (cache-first strategy)
        leaderboardService.updateRestaurantScore(updated);

        logger.info("Updated restaurant {} - Average Score: {}, Review Count: {}",
                restaurant.getName(), averageScore, restaurant.getReviewCount());
    }

    public List<Restaurant> getTopRatedRestaurants() {
        return restaurantRepository.findAllOrderByAverageScoreDesc();
    }

    public List<Restaurant> getMostReviewedRestaurants() {
        return restaurantRepository.findAllOrderByReviewCountDesc();
    }
}
