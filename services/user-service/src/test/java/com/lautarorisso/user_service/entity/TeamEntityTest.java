package com.lautarorisso.user_service.entity;

import com.lautarorisso.user_service.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Team} JPA entity mapping and basic persistence.
 */
@DataJpaTest
@ActiveProfiles("test")
class TeamEntityTest {

    @Autowired
    private TeamRepository teamRepo;

    @Test
    void shouldSaveAndFindTeamById() {
        var entity = new Team();
        entity.setName("SRE");
        entity.setDescription("Site Reliability Engineering");

        var saved = teamRepo.save(entity);
        assertNotNull(saved.getId());

        Optional<Team> found = teamRepo.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SRE", found.get().getName());
        assertEquals("Site Reliability Engineering", found.get().getDescription());
    }
}
