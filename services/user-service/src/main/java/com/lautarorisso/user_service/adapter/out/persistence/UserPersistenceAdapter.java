package com.lautarorisso.user_service.adapter.out.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.mapper.UserEntityMapper;
import com.lautarorisso.user_service.adapter.out.persistence.repository.UserJpaRepository;
import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import com.lautarorisso.user_service.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing UserRepository.
 * <p>
 * Delegates to JPA repository and MapStruct mapper for domain conversion.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;

    @Override
    public User save(User user) {
        var entity = mapper.toEntity(user);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByKeycloakId(UUID keycloakId) {
        return jpaRepository.findByKeycloakId(keycloakId)
                .map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<User> findByTeamId(TeamId teamId) {
        return jpaRepository.findByTeamIdsContaining(teamId.getValue()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UserId id) {
        jpaRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsByKeycloakId(UUID keycloakId) {
        return jpaRepository.existsByKeycloakId(keycloakId);
    }
}
