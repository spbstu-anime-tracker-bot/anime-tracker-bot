package com.animetracker.llm.internal;

import com.animetracker.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.net.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class OllamaService implements LlmService {

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

    @Override
    public List<String> getRecommendations(List<Object[]> watchedAnime) {
        String prompt = buildPrompt(watchedAnime);
        log.info("Ollama request prompt:\n{}", prompt);
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(requestBody, headers), String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            log.error("Ollama API error: {}", e.getMessage());
            throw new RuntimeException("Ollama unavailable", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            Map<String, Object> requestBody = Map.of("model", model, "prompt", "hi", "stream", false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(requestBody, headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
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
        if (!rated.isEmpty()) sb.append("Anime I have watched and rated:\n").append(rated);
        if (!watched.isEmpty()) sb.append("\nAnime I have watched (no rating):\n").append(watched);
        sb.append("\nBased on my watch history, consider the genres, themes, and mood of what I enjoyed most. ");
        sb.append("Recommend exactly 10 anime I have NOT watched that best match my personal taste. ");
        sb.append("If I gave the anime a high rating, then I need to recommend similar anime on the same subject. ");
        sb.append("Format each line as: NUMBER. TITLE. there should be no other text. ");
        sb.append("Use the official title as it appears on MyAnimeList.");
        return sb.toString();
    }

    private List<String> parseResponse(String responseBody) {
        List<String> titles = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("response").asText();
            log.info("Ollama response:\n{}", text);
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                line = line.replaceAll("^\\d+[.)\\-]\\s*", "").trim();
                line = line.replaceAll("\\.$", "").trim();
                if (!line.isEmpty()) titles.add(line);
            }
        } catch (Exception e) {
            log.error("Failed to parse Ollama response: {}", e.getMessage());
        }
        return titles;
    }
}
