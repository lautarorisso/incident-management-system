package com.lautarorisso.user_service.repository;

import com.lautarorisso.user_service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link UserRepository} query methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepo;

    @Test
    void shouldFindByTeamId() {
        UUID teamId = UUID.randomUUID();
        persistUser("user1", teamId);
        persistUser("user2", null);

        List<User> found = userRepo.findByTeamIdsContaining(teamId);

        assertEquals(1, found.size());
        assertEquals("user1", found.getFirst().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenTeamHasNoUsers() {
        List<User> found = userRepo.findByTeamIdsContaining(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    private void persistUser(String username, UUID teamId) {
        var builder = User.builder()
                .keycloakId(UUID.randomUUID())
                .username(username)
                .displayName("Display " + username)
                .email(username + "@example.com")
                .active(true);
        if (teamId != null) {
            builder.teamIds(List.of(teamId));
        }
        userRepo.save(builder.build());
    }
}
