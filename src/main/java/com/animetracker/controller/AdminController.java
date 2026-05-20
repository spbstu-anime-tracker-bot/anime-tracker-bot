package com.animetracker.controller;

import com.animetracker.dto.UserStatsDto;
import com.animetracker.module.ListUsersModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ListUsersModule listUsersModule;

    @Value("${admin.token}")
    private String adminToken;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized. Provide valid Bearer token."));
        }

        List<UserStatsDto> users = listUsersModule.getAllUsersStats();
        return ResponseEntity.ok(users);
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.substring("Bearer ".length()).trim();
        return adminToken.equals(token);
    }
}
