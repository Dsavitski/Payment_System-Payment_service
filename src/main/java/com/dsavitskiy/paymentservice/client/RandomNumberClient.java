package com.dsavitskiy.paymentservice.client;

import com.dsavitskiy.paymentservice.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestTemplate restTemplate;
    private static final String API_URL = "https://random-data-api.com/api/v2/numbers";

    public int getRandomNumber() {
        try {
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);

            if (response != null && response.containsKey("value")) {
                int number = ((Number) response.get("value")).intValue();
                log.info("Getting random number is successful: {}", number);
                return number;
            }
            throw new ExternalApiException("Uncorrect format response", null);
        } catch (Exception e) {
            log.info("Error calling external API: {}", e.getMessage());
            throw new ExternalApiException("External API error", e);
        }
    }
}