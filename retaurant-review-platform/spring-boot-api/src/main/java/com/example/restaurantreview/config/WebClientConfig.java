package com.example.restaurantreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${app.nlp.service-url}")
    private String nlpServiceUrl;

    @Value("${app.nlp.timeout:30s}")
    private Duration timeout;

    @Bean
    public WebClient nlpWebClient() {
        return WebClient.builder()
                .baseUrl(nlpServiceUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
}
