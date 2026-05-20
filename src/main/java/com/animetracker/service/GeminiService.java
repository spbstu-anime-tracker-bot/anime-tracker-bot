package com.animetracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper;

    public GeminiService(
            RestTemplateBuilder builder,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl,
            @Value("${gemini.api.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.objectMapper = objectMapper;
    }

    /**
     
     
     */
    public List<String> getRecommendations(List<Object[]> ratedAnime) {
        String prompt = buildPrompt(ratedAnime);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = apiUrl + "?key=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseGeminiResponse(response.getBody());
        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage());
            throw new RuntimeException("Gemini unavailable", e);
        }
    }

    private String buildPrompt(List<Object[]> ratedAnime) {
        StringBuilder sb = new StringBuilder();
        sb.append("I have watched these anime and rated them:\n");
        for (Object[] row : ratedAnime) {
            String title = (String) row[2];
            Object score = row[1];
            sb.append("- ").append(title).append(": ").append(score).append("/10\n");
        }
        sb.append("\nBased on my ratings, recommend exactly 10 anime I have NOT watched yet. ");
        sb.append("Return ONLY a numbered list of anime titles (1-10), one per line, no explanations. ");
        sb.append("Use the official title as it appears on MyAnimeList.");
        return sb.toString();
    }

    private List<String> parseGeminiResponse(String responseBody) {
        List<String> titles = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                line = line.replaceAll("^\\d+[.)\\-]\\s*", "").trim();
                if (!line.isEmpty()) {
                    titles.add(line);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
        }
        return titles;
    }

    public boolean isAvailable() {
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", "Say OK"))
                    ))
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl + "?key=" + apiKey, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
