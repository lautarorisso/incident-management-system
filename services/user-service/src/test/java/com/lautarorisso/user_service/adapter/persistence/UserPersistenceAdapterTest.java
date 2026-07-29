package com.lautarorisso.user_service.adapter.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.UserPersistenceAdapter;
import com.lautarorisso.user_service.adapter.out.persistence.mapper.UserEntityMapper;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import({UserPersistenceAdapterTest.TestConfig.class, UserPersistenceAdapter.class})
class UserPersistenceAdapterTest {

    @Autowired
    private UserPersistenceAdapter adapter;

    @TestConfiguration
    static class TestConfig {
        @Bean
        UserEntityMapper userEntityMapper() {
            return Mappers.getMapper(UserEntityMapper.class);
        }
    }

    @Test
    void savesAndFindsUserById() {
        var user = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .build();

        var saved = adapter.save(user);
        assertNotNull(saved.getId());

        var found = adapter.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("jdoe", found.get().getUsername());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        var found = adapter.findById(new UserId(UUID.randomUUID()));
        assertTrue(found.isEmpty());
    }

    @Test
    void findByKeycloakIdReturnsUser() {
        var keycloakId = UUID.randomUUID();
        var user = User.builder()
                .keycloakId(keycloakId)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .build();

        adapter.save(user);

        var found = adapter.findByKeycloakId(keycloakId);
        assertTrue(found.isPresent());
    }

    @Test
    void findAllReturnsAllUsers() {
        adapter.save(User.builder().keycloakId(UUID.randomUUID()).username("u1").displayName("U1").email("u1@test.com").build());
        adapter.save(User.builder().keycloakId(UUID.randomUUID()).username("u2").displayName("U2").email("u2@test.com").build());

        var all = adapter.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void findByTeamIdReturnsUsersInTeam() {
        var teamId = new TeamId(UUID.randomUUID());
        var user = User.builder()
                .keycloakId(UUID.randomUUID())
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .teamIds(List.of(teamId))
                .build();

        adapter.save(user);

        var found = adapter.findByTeamId(teamId);
        assertEquals(1, found.size());
    }

    @Test
    void existsByKeycloakIdReturnsTrueForExisting() {
        var keycloakId = UUID.randomUUID();
        adapter.save(User.builder()
                .keycloakId(keycloakId)
                .username("jdoe")
                .displayName("John Doe")
                .email("john@example.com")
                .build());

        assertTrue(adapter.existsByKeycloakId(keycloakId));
    }
}
