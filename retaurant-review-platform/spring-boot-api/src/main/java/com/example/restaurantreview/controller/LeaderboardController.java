package com.example.restaurantreview.controller;

import com.example.restaurantreview.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardService.RestaurantLeaderboardEntry>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {

        List<LeaderboardService.RestaurantLeaderboardEntry> leaderboard =
                leaderboardService.getTopRestaurants(limit);

        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/restaurant/{restaurantId}/rank")
    public ResponseEntity<?> getRestaurantRank(@PathVariable Long restaurantId) {
        Long rank = leaderboardService.getRestaurantRank(restaurantId);

        if (rank != null) {
            return ResponseEntity.ok(Map.of(
                    "restaurantId", restaurantId,
                    "rank", rank,
                    "message", "Restaurant is ranked #" + rank + " on the leaderboard"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "restaurantId", restaurantId,
                    "rank", null,
                    "message", "Restaurant not found in leaderboard"
            ));
        }
    }
}
