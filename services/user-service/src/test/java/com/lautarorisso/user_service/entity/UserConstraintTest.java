package com.lautarorisso.user_service.entity;

import com.lautarorisso.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that database constraints on {@link User} are enforced.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserConstraintTest {

    @Autowired
    private UserRepository userRepo;

    @Test
    void shouldRejectDuplicateUsername() {
        userRepo.save(User.builder()
                .keycloakId(UUID.randomUUID())
                .username("duplicate")
                .displayName("First")
                .email("first@example.com")
                .active(true)
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepo.saveAndFlush(User.builder()
                        .keycloakId(UUID.randomUUID())
                        .username("duplicate")
                        .displayName("Second")
                        .email("second@example.com")
                        .active(true)
                        .build()));
    }

    @Test
    void shouldRejectDuplicateKeycloakId() {
        UUID sharedKeycloakId = UUID.randomUUID();

        userRepo.save(User.builder()
                .keycloakId(sharedKeycloakId)
                .username("user1")
                .displayName("First")
                .email("first@example.com")
                .active(true)
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepo.saveAndFlush(User.builder()
                        .keycloakId(sharedKeycloakId)
                        .username("user2")
                        .displayName("Second")
                        .email("second@example.com")
                        .active(true)
                        .build()));
    }
}
