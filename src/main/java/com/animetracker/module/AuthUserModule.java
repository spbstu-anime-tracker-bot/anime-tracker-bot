package com.animetracker.module;

import com.animetracker.user.User;
import com.animetracker.user.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthUserModule {

    private final UserRegistrationService userRegistrationService;

    public User registerOrGet(Long telegramId, String name) {
        return userRegistrationService.registerOrGet(telegramId, name);
    }

    public boolean exists(Long telegramId) {
        return userRegistrationService.exists(telegramId);
    }
}
