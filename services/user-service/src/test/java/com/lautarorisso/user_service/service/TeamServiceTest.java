package com.lautarorisso.user_service.service;

import com.lautarorisso.user_service.entity.Team;
import com.lautarorisso.user_service.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    void shouldGetTeamById() {
        UUID id = UUID.randomUUID();
        Team team = Team.builder().id(id).name("SRE").build();

        when(teamRepository.findById(id)).thenReturn(Optional.of(team));

        Optional<Team> found = teamService.getTeamById(id);

        assertTrue(found.isPresent());
        assertEquals("SRE", found.get().getName());
        verify(teamRepository).findById(id);
    }

    @Test
    void shouldReturnEmptyWhenTeamNotFound() {
        UUID id = UUID.randomUUID();

        when(teamRepository.findById(id)).thenReturn(Optional.empty());

        assertTrue(teamService.getTeamById(id).isEmpty());
    }
}
