package com.lautarorisso.user_service.adapter.out.persistence.repository;

import com.lautarorisso.user_service.adapter.out.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for TeamEntity.
 */
@Repository
public interface TeamJpaRepository extends JpaRepository<TeamEntity, UUID> {

    Optional<TeamEntity> findByName(String name);

    boolean existsByName(String name);
}
