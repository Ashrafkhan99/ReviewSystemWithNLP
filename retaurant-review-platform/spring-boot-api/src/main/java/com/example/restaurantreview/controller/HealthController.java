package com.example.restaurantreview.controller;

import com.example.restaurantreview.service.NLPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NLPService nlpService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("status", "UP");

        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN - " + e.getMessage());
        }

        // Check Redis connectivity
        try {
            redisTemplate.opsForValue().set("health-check", "ok");
            String result = (String) redisTemplate.opsForValue().get("health-check");
            health.put("redis", "ok".equals(result) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("redis", "DOWN - " + e.getMessage());
        }

        // Check NLP service
        boolean nlpHealthy = nlpService.isNLPServiceHealthy();
        health.put("nlp-service", nlpHealthy ? "UP" : "DOWN");

        return ResponseEntity.ok(health);
    }
}
