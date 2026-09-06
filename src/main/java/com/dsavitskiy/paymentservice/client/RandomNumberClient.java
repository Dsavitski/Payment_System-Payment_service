package com.dsavitskiy.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestTemplate restTemplate;

    @Value("${external.api.random-number.url}")
    private String apiUrl;

    public int getRandomNumber() {
        log.debug("Fetching random number from: {}", apiUrl);
        try {
            Integer number = restTemplate.getForObject(apiUrl, Integer.class);
            return number != null ? number : 0;
        } catch (Exception e) {
            log.error("Failed to fetch random number from {}", apiUrl, e);
            throw new com.dsavitskiy.paymentservice.exception.ExternalApiException(
                    "External API unavailable: ", e);
        }
    }
}