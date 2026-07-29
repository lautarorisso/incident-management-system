package com.lautarorisso.incident_service.adapter.in.rest.config;

import com.lautarorisso.incident_service.domain.service.IncidentDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers pure domain services as Spring beans so they can be injected
 * into application-level use case implementations.
 * <p>
 * Domain services intentionally carry no framework annotations — this keeps
 * the domain layer pure and testable without Spring.
 */
@Configuration
public class DomainConfig {

    @Bean
    public IncidentDomainService incidentDomainService() {
        return new IncidentDomainService();
    }
}
