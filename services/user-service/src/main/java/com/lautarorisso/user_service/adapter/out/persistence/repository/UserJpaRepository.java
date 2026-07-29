package com.lautarorisso.user_service.adapter.out.persistence.repository;

import com.lautarorisso.user_service.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserEntity.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByKeycloakId(UUID keycloakId);

    List<UserEntity> findByTeamIdsContaining(UUID teamId);

    boolean existsByKeycloakId(UUID keycloakId);
}
