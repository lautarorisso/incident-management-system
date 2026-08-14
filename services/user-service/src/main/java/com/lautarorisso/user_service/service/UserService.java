package com.lautarorisso.user_service.service;

import com.lautarorisso.user_service.entity.User;
import com.lautarorisso.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer service for querying users.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getUsers(UUID teamId) {
        if (teamId != null) {
            return userRepository.findByTeamIdsContaining(teamId);
        }
        return userRepository.findAll();
    }
}
