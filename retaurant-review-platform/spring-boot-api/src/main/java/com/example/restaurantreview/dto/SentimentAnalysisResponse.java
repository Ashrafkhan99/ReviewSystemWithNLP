package com.example.restaurantreview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SentimentAnalysisResponse {

    private String sentiment;
    private Double confidence;
    private Double score;

    @JsonProperty("is_positive")
    private Boolean isPositive;

    @JsonProperty("processed_at")
    private String processedAt;

    // Constructors
    public SentimentAnalysisResponse() {}

    public SentimentAnalysisResponse(String sentiment, Double confidence, Double score, Boolean isPositive) {
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.score = score;
        this.isPositive = isPositive;
    }

    // Getters and Setters
    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Boolean getIsPositive() {
        return isPositive;
    }

    public void setIsPositive(Boolean isPositive) {
        this.isPositive = isPositive;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}
