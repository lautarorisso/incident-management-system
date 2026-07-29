package com.lautarorisso.user_service.adapter.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.entity.UserEntity;
import com.lautarorisso.user_service.adapter.out.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserEntityMappingTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void savesAndFindsUserById() {
        var entity = new UserEntity();
        entity.setKeycloakId(UUID.randomUUID());
        entity.setUsername("jdoe");
        entity.setDisplayName("John Doe");
        entity.setEmail("john@example.com");
        entity.setActive(true);

        var saved = userJpaRepository.save(entity);
        assertNotNull(saved.getId());

        var found = userJpaRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("jdoe", found.get().getUsername());
    }

    @Test
    void findsUserByKeycloakId() {
        var keycloakId = UUID.randomUUID();
        var entity = new UserEntity();
        entity.setKeycloakId(keycloakId);
        entity.setUsername("jdoe");
        entity.setDisplayName("John Doe");
        entity.setEmail("john@example.com");
        entity.setActive(true);
        userJpaRepository.save(entity);

        var found = userJpaRepository.findByKeycloakId(keycloakId);
        assertTrue(found.isPresent());
        assertEquals("jdoe", found.get().getUsername());
    }

    @Test
    void returnsEmptyWhenKeycloakIdNotFound() {
        var found = userJpaRepository.findByKeycloakId(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }
}
