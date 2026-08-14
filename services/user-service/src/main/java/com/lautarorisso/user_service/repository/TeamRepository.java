package com.lautarorisso.user_service.repository;

import com.lautarorisso.user_service.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for {@link Team} entities.
 */
public interface TeamRepository extends JpaRepository<Team, UUID> {
}
