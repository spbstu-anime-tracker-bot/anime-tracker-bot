package com.animetracker.controller;

import com.animetracker.module.HealthCheckModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckModule healthCheckModule;

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = healthCheckModule.checkHealth();
        boolean degraded = "DEGRADED".equals(status.get("status"));
        return degraded
                ? ResponseEntity.status(503).body(status)
                : ResponseEntity.ok(status);
    }
}
