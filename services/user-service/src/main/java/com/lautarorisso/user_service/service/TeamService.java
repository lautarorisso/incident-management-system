package com.lautarorisso.user_service.service;

import com.lautarorisso.user_service.entity.Team;
import com.lautarorisso.user_service.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer service for querying teams.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Optional<Team> getTeamById(UUID id) {
        return teamRepository.findById(id);
    }
}
