package com.lautarorisso.user_service.adapter.out.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.mapper.TeamEntityMapper;
import com.lautarorisso.user_service.adapter.out.persistence.repository.TeamJpaRepository;
import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.port.out.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing TeamRepository.
 * <p>
 * Delegates to JPA repository and MapStruct mapper for domain conversion.
 */
@Component
@RequiredArgsConstructor
public class TeamPersistenceAdapter implements TeamRepository {

    private final TeamJpaRepository jpaRepository;
    private final TeamEntityMapper mapper;

    @Override
    public Team save(Team team) {
        var entity = mapper.toEntity(team);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Team> findById(TeamId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Team> findByName(String name) {
        return jpaRepository.findByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<Team> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(TeamId id) {
        jpaRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }
}
