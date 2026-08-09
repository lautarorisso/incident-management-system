package com.lautarorisso.incident_service.adapter.persistence.repository;

import com.lautarorisso.incident_service.adapter.persistence.entity.IncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for IncidentEntity.
 * <p>
 * Query methods here cover the filter/pagination combinations used by the
 * {@code IncidentPersistenceAdapter}. Physical indexes backing these queries are
 * created via Flyway migration V2 (partial/covering/concurrent) rather than
 * JPA {@code @Index} annotations.
 */
@Repository
public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {

    Page<IncidentEntity> findByStatusAndPriorityOrderByCreatedAtDesc(String status, String priority, Pageable pageable);

    Page<IncidentEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<IncidentEntity> findByPriorityOrderByCreatedAtDesc(String priority, Pageable pageable);

    Page<IncidentEntity> findByAssigneeIdAndStatusOrderByCreatedAtDesc(UUID assigneeId, String status, Pageable pageable);

    Page<IncidentEntity> findByTeamIdAndStatusOrderByCreatedAtDesc(UUID teamId, String status, Pageable pageable);

    Page<IncidentEntity> findByAssigneeIdOrderByCreatedAtDesc(UUID assigneeId, Pageable pageable);

    Page<IncidentEntity> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    Page<IncidentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatusAndPriority(String status, String priority);

    long countByTeamIdAndStatus(UUID teamId, String status);

    long countByAssigneeIdAndStatus(UUID assigneeId, String status);
}
