package com.lautarorisso.user_service.entity;

import com.lautarorisso.user_service.repository.TeamRepository;
import com.lautarorisso.user_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Team} JPA entity mapping and basic persistence.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TeamEntityTest extends AbstractPostgresTestBase {

    @Autowired
    private TeamRepository teamRepo;

    @Test
    void shouldSaveAndFindTeamById() {
        var entity = Team.builder()
                .name("SRE")
                .description("Site Reliability Engineering")
                .build();

        var saved = teamRepo.save(entity);
        assertNotNull(saved.getId());

        Optional<Team> found = teamRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SRE", found.get().getName());
        assertEquals("Site Reliability Engineering", found.get().getDescription());
    }
}
