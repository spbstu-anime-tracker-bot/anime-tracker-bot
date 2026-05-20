package com.animetracker.module;

import com.animetracker.entity.User;
import com.animetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserModule {

    private final UserRepository userRepository;

    @Transactional
    public User registerOrGet(Long telegramId, String name) {
        return userRepository.findById(telegramId).orElseGet(() -> {
            User user = new User();
            user.setId(telegramId);
            user.setName(name);
            log.info("Registering new user: {}", telegramId);
            return userRepository.save(user);
        });
    }

    public boolean exists(Long telegramId) {
        return userRepository.existsById(telegramId);
    }
}
