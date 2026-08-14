-- V2: Índices para las consultas reales del IncidentRepository y OutboxPoller.
-- Cada índice cubre el WHERE + ORDER BY de una query de la app.

-- findByStatusAndPriorityOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_incidents_status_priority_created
    ON incidents (status, priority, created_at DESC);

-- findByAssigneeIdAndStatusOrderByCreatedAtDesc / findByAssigneeIdOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_incidents_assignee_status_created
    ON incidents (assignee_id, status, created_at DESC)
    WHERE assignee_id IS NOT NULL;

-- findByTeamIdAndStatusOrderByCreatedAtDesc / findByTeamIdOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_incidents_team_status_created
    ON incidents (team_id, status, created_at DESC)
    WHERE team_id IS NOT NULL;

-- findByPublishedFalse (OutboxPoller)
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_events (created_at)
    WHERE NOT published;
