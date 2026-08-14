package com.lautarorisso.user_service.repository;

import com.lautarorisso.user_service.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TeamRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepo;

    @Test
    void shouldSaveAndFindById() {
        Team entity = new Team();
        entity.setName("SRE");
        entity.setDescription("Site Reliability Engineering");
        Team saved = teamRepo.save(entity);

        Optional<Team> found = teamRepo.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("SRE", found.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenIdNotFound() {
        Optional<Team> found = teamRepo.findById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }
}
