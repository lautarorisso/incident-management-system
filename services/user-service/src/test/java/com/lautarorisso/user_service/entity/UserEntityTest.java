package com.lautarorisso.user_service.entity;

import com.lautarorisso.user_service.repository.UserRepository;
import com.lautarorisso.user_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link User} JPA entity mapping and basic persistence.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserEntityTest extends AbstractPostgresTestBase {

    @Autowired
    private UserRepository userRepo;

    @Test
    void shouldSaveAndFindUserById() {
        var entity = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .active(true)
                .build();

        var saved = userRepo.save(entity);
        assertNotNull(saved.getId());

        Optional<User> found = userRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("jdoe", found.get().getUsername());
        assertEquals("John Doe", found.get().getDisplayName());
        assertEquals("john@example.com", found.get().getEmail());
        assertTrue(found.get().isActive());
    }

    @Test
    void shouldPersistTeamIds() {
        UUID teamId = UUID.randomUUID();
        var entity = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .active(true)
                .teamIds(List.of(teamId))
                .build();

        var saved = userRepo.save(entity);
        var found = userRepo.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(List.of(teamId), found.get().getTeamIds());
    }
}
