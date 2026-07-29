package com.lautarorisso.incident_service.adapter.persistence.repository;

import com.lautarorisso.incident_service.adapter.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for IncidentEntity.
 */
@Repository
public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {
}
