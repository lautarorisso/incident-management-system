package com.lautarorisso.notification_service.adapter.out.persistence;

import com.lautarorisso.notification_service.adapter.out.persistence.mapper.ProcessedEventEntityMapper;
import com.lautarorisso.notification_service.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import com.lautarorisso.notification_service.domain.model.ProcessedEvent;
import com.lautarorisso.notification_service.domain.port.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persistence adapter implementing ProcessedEventRepository for idempotency tracking.
 */
@Component
@RequiredArgsConstructor
public class ProcessedEventPersistenceAdapter implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpaRepository;
    private final ProcessedEventEntityMapper mapper;

    @Override
    @Transactional
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        var entity = mapper.toEntity(processedEvent);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedEvent> findByEventId(String eventId) {
        return jpaRepository.findById(eventId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEventId(String eventId) {
        return jpaRepository.existsById(eventId);
    }
}
