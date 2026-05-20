package com.animetracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;

    public OllamaService(
            @Value("${ollama.api.url}") String apiUrl,
            @Value("${ollama.api.model:gemma3}") String model,
            @Value("${ollama.api.timeout-seconds:120}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.apiUrl = apiUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setProxy(Proxy.NO_PROXY);
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    public List<String> getRecommendations(List<Object[]> ratedAnime) {
        String prompt = buildPrompt(ratedAnime);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
            return parseResponse(response.getBody());
        } catch (Exception e) {
            log.error("Ollama API error: {}", e.getMessage());
            throw new RuntimeException("Ollama unavailable", e);
        }
    }

    private String buildPrompt(List<Object[]> watchedAnime) {
        StringBuilder rated = new StringBuilder();
        StringBuilder watched = new StringBuilder();

        for (Object[] row : watchedAnime) {
            String title = (String) row[2];
            Object score = row[1];
            if (score != null) {
                rated.append("- ").append(title).append(": ").append(score).append("/10\n");
            } else {
                watched.append("- ").append(title).append("\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!rated.isEmpty()) {
            sb.append("Anime I have watched and rated:\n").append(rated);
        }
        if (!watched.isEmpty()) {
            sb.append("\nAnime I have watched (no rating):\n").append(watched);
        }
        sb.append("\nBased on my watch history, recommend exactly 10 anime I have NOT watched yet. ");
        sb.append("Return ONLY a numbered list of anime titles (1-10), one per line, no explanations. ");
        sb.append("Use the official title as it appears on MyAnimeList.");
        return sb.toString();
    }

    public boolean isAvailable() {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", "hi",
                    "stream", false
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(requestBody, headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> parseResponse(String responseBody) {
        List<String> titles = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("response").asText();
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                line = line.replaceAll("^\\d+[.)\\-]\\s*", "").trim();
                if (!line.isEmpty()) {
                    titles.add(line);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Ollama response: {}", e.getMessage());
        }
        return titles;
    }
}
