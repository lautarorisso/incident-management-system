package com.lautarorisso.user_service.repository;

import com.lautarorisso.user_service.entity.Team;
import com.lautarorisso.user_service.entity.User;
import com.lautarorisso.user_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link UserRepository} query methods.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest extends AbstractPostgresTestBase {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TeamRepository teamRepo;

    @Test
    void shouldFindByTeamId() {
        UUID teamId = teamRepo.save(Team.builder()
                .name("SRE")
                .description("Site Reliability Engineering")
                .build()).getId();
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
