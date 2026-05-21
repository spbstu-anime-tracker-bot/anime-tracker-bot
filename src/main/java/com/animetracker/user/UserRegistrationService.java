package com.animetracker.user;

import com.animetracker.user.internal.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

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

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
