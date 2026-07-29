package com.lautarorisso.incident_service.adapter.persistence.repository;

import com.lautarorisso.incident_service.adapter.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for OutboxEventEntity.
 */
@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByPublishedFalse();
}
