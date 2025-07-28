package com.example.restaurantreview.controller;

import com.example.restaurantreview.dto.RestaurantRequest;
import com.example.restaurantreview.entity.Restaurant;
import com.example.restaurantreview.service.LeaderboardService;
import com.example.restaurantreview.service.RestaurantService;
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
@RequestMapping("/api/restaurants")
@CrossOrigin(origins = "*")
public class RestaurantController {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantController.class);

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private LeaderboardService leaderboardService;

    @PostMapping
    public ResponseEntity<?> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        try {
            Restaurant restaurant = restaurantService.createRestaurant(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(restaurant);
        } catch (RuntimeException e) {
            logger.error("Error creating restaurant: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<Restaurant>> getAllRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Restaurant> restaurants = restaurantService.searchRestaurants(search, pageable);
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRestaurantById(@PathVariable Long id) {
        return restaurantService.getRestaurantById(id)
                .map(restaurant -> {
                    // Add rank information
                    Long rank = leaderboardService.getRestaurantRank(id);
                    Map<String, Object> response = Map.of(
                            "restaurant", restaurant,
                            "leaderboardRank", rank != null ? rank : "Not ranked"
                    );
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRestaurant(@PathVariable Long id,
                                              @Valid @RequestBody RestaurantRequest request) {
        try {
            Restaurant updated = restaurantService.updateRestaurant(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("Error updating restaurant: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRestaurant(@PathVariable Long id) {
        try {
            restaurantService.deleteRestaurant(id);
            return ResponseEntity.ok(Map.of("message", "Restaurant deleted successfully"));
        } catch (RuntimeException e) {
            logger.error("Error deleting restaurant: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cuisine/{cuisineType}")
    public ResponseEntity<List<Restaurant>> getRestaurantsByCuisine(@PathVariable String cuisineType) {
        List<Restaurant> restaurants = restaurantService.getRestaurantsByCuisine(cuisineType);
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<Restaurant>> getTopRatedRestaurants() {
        List<Restaurant> restaurants = restaurantService.getTopRatedRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/most-reviewed")
    public ResponseEntity<List<Restaurant>> getMostReviewedRestaurants() {
        List<Restaurant> restaurants = restaurantService.getMostReviewedRestaurants();
        return ResponseEntity.ok(restaurants);
    }
}
