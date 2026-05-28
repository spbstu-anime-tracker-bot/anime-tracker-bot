package com.animetracker.llm.internal;

import com.animetracker.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Primary
@Service
public class GigaChatService implements LlmService {

    private static final String AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";

    private final String authKey;
    private final String model;
    private final String scope;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private String accessToken;
    private long tokenExpiresAt = 0;

    public GigaChatService(
            @Value("${GIGACHAT_AUTH_KEY:}") String authKey,
            @Value("${GIGACHAT_MODEL:GigaChat}") String model,
            @Value("${GIGACHAT_SCOPE:GIGACHAT_API_PERS}") String scope,
            ObjectMapper objectMapper) {
        this.authKey = authKey;
        this.model = model;
        this.scope = scope;
        this.objectMapper = objectMapper;
        this.restTemplate = createRestTemplate();
    }

    @Override
    public List<String> getRecommendations(List<Object[]> watchedAnime) {
        String prompt = buildPrompt(watchedAnime);
        log.info("GigaChat request prompt:\n{}", prompt);

        ensureValidToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    CHAT_URL, new HttpEntity<>(requestBody, headers), String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            log.error("GigaChat API error: {}", e.getMessage());
            throw new RuntimeException("GigaChat unavailable", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            ensureValidToken();
            return accessToken != null;
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized void ensureValidToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) {
            return;
        }
        refreshToken();
    }

    private void refreshToken() {
        log.info("GigaChat auth attempt — key prefix: [{}], scope: [{}]",
                authKey.length() > 12 ? authKey.substring(0, 12) + "..." : "(empty or too short)",
                scope);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + authKey);
        headers.set("RqUID", UUID.randomUUID().toString());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("scope", scope);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    AUTH_URL, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            accessToken = root.path("access_token").asText();
            tokenExpiresAt = root.path("expires_at").asLong();
            log.info("GigaChat token refreshed successfully");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("GigaChat auth failed: status={}, body=[{}]", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("GigaChat auth failed", e);
        } catch (Exception e) {
            log.error("Failed to get GigaChat token: {}", e.getMessage());
            throw new RuntimeException("GigaChat auth failed", e);
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
            String text = root.path("choices").get(0).path("message").path("content").asText();
            log.info("GigaChat response:\n{}", text);
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                line = line.replaceAll("^\\d+[.)\\-]\\s*", "").trim();
                line = line.replaceAll("\\.$", "").trim();
                if (!line.isEmpty()) titles.add(line);
            }
        } catch (Exception e) {
            log.error("Failed to parse GigaChat response: {}", e.getMessage());
        }
        return titles;
    }

    private RestTemplate createRestTemplate() {
        try {
            TrustManager[] trustAll = {new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection https) {
                        https.setSSLSocketFactory(sslSocketFactory);
                        https.setHostnameVerifier((host, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(120_000);
            return new RestTemplate(factory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SSL for GigaChat", e);
        }
    }
}
