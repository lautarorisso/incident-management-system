package com.lautarorisso.user_service.adapter.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.entity.TeamEntity;
import com.lautarorisso.user_service.adapter.out.persistence.repository.TeamJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TeamEntityMappingTest {

    @Autowired
    private TeamJpaRepository teamJpaRepository;

    @Test
    void savesAndFindsTeamById() {
        var entity = new TeamEntity();
        entity.setName("SRE");
        entity.setDescription("Site Reliability Engineering");

        var saved = teamJpaRepository.save(entity);
        assertNotNull(saved.getId());

        var found = teamJpaRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SRE", found.get().getName());
    }

    @Test
    void findsTeamByName() {
        var entity = new TeamEntity();
        entity.setName("SRE");
        entity.setDescription("Site Reliability Engineering");
        teamJpaRepository.save(entity);

        var found = teamJpaRepository.findByName("SRE");
        assertTrue(found.isPresent());
    }

    @Test
    void returnsEmptyWhenNameNotFound() {
        var found = teamJpaRepository.findByName("NonExistent");
        assertTrue(found.isEmpty());
    }
}
