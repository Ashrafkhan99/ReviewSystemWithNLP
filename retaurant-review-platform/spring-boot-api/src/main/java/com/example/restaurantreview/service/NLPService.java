package com.example.restaurantreview.service;

import com.example.restaurantreview.dto.SentimentAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class NLPService {

    private static final Logger logger = LoggerFactory.getLogger(NLPService.class);

    @Autowired
    private WebClient nlpWebClient;

    public SentimentAnalysisResponse analyzeSentiment(String text) {
        try {
            logger.info("Analyzing sentiment for text: {}", text.substring(0, Math.min(text.length(), 50)) + "...");

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", text);

            SentimentAnalysisResponse response = nlpWebClient
                    .post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatus.SERVICE_UNAVAILABLE::equals,
                            clientResponse -> {
                                logger.warn("NLP service not ready, will retry...");
                                return Mono.error(new ServiceNotReadyException("NLP model still loading"));
                            })
                    .bodyToMono(SentimentAnalysisResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                            .filter(throwable -> throwable instanceof ServiceNotReadyException))
                    .timeout(Duration.ofSeconds(6000)) // Increased timeout
                    .block();

            logger.info("Sentiment analysis completed: {} (confidence: {})",
                    response.getSentiment(), response.getConfidence());

            return response;

        } catch (WebClientResponseException e) {
            logger.error("HTTP error calling NLP service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return createFallbackResponse();
        } catch (Exception e) {
            logger.error("Unexpected error during sentiment analysis: {}", e.getMessage());
            return createFallbackResponse();
        }
    }

    public boolean isNLPServiceReady() {
        try {
            Map<String, Object> healthStatus = nlpWebClient
                    .get()
                    .uri("/ready")  // Use the readiness endpoint
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            Boolean modelReady = (Boolean) healthStatus.get("model_ready");
            return Boolean.TRUE.equals(modelReady);

        } catch (Exception e) {
            logger.warn("NLP service readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isNLPServiceHealthy() {
        try {
            return nlpWebClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .map(response -> "healthy".equals(response.get("status")) || "degraded".equals(response.get("status")))
                    .onErrorReturn(false)
                    .block();
        } catch (Exception e) {
            logger.warn("NLP service health check failed: {}", e.getMessage());
            return false;
        }
    }

    private SentimentAnalysisResponse createFallbackResponse() {
        logger.warn("Using fallback sentiment analysis response");
        SentimentAnalysisResponse fallback = new SentimentAnalysisResponse();
        fallback.setSentiment("NEUTRAL");
        fallback.setConfidence(0.5);
        fallback.setScore(0.0);
        fallback.setIsPositive(null);
        return fallback;
    }

    // Custom exception for service not ready scenarios
    public static class ServiceNotReadyException extends RuntimeException {
        public ServiceNotReadyException(String message) {
            super(message);
        }
    }
}
