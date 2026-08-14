package com.lautarorisso.user_service.entity;

import com.lautarorisso.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles("test")
class UserEntityTest {

    @Autowired
    private UserRepository userRepo;

    @Test
    void shouldSaveAndFindUserById() {
        var entity = new User();
        entity.setKeycloakId(UUID.randomUUID());
        entity.setUsername("jdoe");
        entity.setDisplayName("John Doe");
        entity.setEmail("john@example.com");
        entity.setActive(true);

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
        var entity = new User();
        entity.setKeycloakId(UUID.randomUUID());
        entity.setUsername("jdoe");
        entity.setDisplayName("John Doe");
        entity.setEmail("john@example.com");
        entity.setActive(true);
        entity.setTeamIds(List.of(teamId));

        var saved = userRepo.save(entity);
        var found = userRepo.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(List.of(teamId), found.get().getTeamIds());
    }
}
