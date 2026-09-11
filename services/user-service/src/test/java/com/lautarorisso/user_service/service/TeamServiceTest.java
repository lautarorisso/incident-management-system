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

/**
 * Unit tests for {@link TeamService} with a mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    void shouldReturnEmptyOptionalWhenTeamNotFound() {
        UUID teamId = UUID.randomUUID();
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        Optional<Team> result = teamService.getTeamById(teamId);

        assertTrue(result.isEmpty(), "Not-found contract is Optional.empty(), not an exception");
        verify(teamRepository).findById(teamId);
    }
}