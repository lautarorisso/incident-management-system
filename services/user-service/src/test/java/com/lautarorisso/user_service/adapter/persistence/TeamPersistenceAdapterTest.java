package com.lautarorisso.user_service.adapter.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.TeamPersistenceAdapter;
import com.lautarorisso.user_service.adapter.out.persistence.mapper.TeamEntityMapper;
import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import({TeamPersistenceAdapterTest.TestConfig.class, TeamPersistenceAdapter.class})
class TeamPersistenceAdapterTest {

    @Autowired
    private TeamPersistenceAdapter adapter;

    @TestConfiguration
    static class TestConfig {
        @Bean
        TeamEntityMapper teamEntityMapper() {
            return Mappers.getMapper(TeamEntityMapper.class);
        }
    }

    @Test
    void savesAndFindsTeamById() {
        var team = Team.builder()
                .name("SRE")
                .description("Site Reliability Engineering")
                .build();

        var saved = adapter.save(team);
        assertNotNull(saved.getId());

        var found = adapter.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SRE", found.get().getName());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        var found = adapter.findById(new TeamId(UUID.randomUUID()));
        assertTrue(found.isEmpty());
    }

    @Test
    void findByNameReturnsTeam() {
        adapter.save(Team.builder().name("SRE").description("SRE team").build());

        var found = adapter.findByName("SRE");
        assertTrue(found.isPresent());
    }

    @Test
    void findAllReturnsAllTeams() {
        adapter.save(Team.builder().name("Team1").build());
        adapter.save(Team.builder().name("Team2").build());

        var all = adapter.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void existsByNameReturnsTrueForExisting() {
        adapter.save(Team.builder().name("SRE").build());

        assertTrue(adapter.existsByName("SRE"));
    }
}
