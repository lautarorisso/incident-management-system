package com.lautarorisso.incident_service.repository;

import com.lautarorisso.incident_service.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Spring Data repository for {@link Incident} entities.
 * <p>
 * Filtering is done via {@link JpaSpecificationExecutor} so any combination
 * of status/priority/assignee/team filters can be applied without deriving
 * an exhaustive set of query methods. The physical indexes backing the
 * filtered queries are created via Flyway migration V2.
 */
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {
}
