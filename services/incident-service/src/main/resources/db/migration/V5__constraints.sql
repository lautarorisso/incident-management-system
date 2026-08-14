-- V5: CHECK constraints a nivel BD + trigger de mantenimiento de updated_at.
-- Validación de datos aunque vengan de SQL raw/scripts/migraciones.
-- Nota: ejecuta solo sobre bases frescas (en bases con historial previo la
-- versión ya aplicada como stub se re-sincroniza con flyway repair sin
-- re-ejecutarse; la app setea updated_at explícitamente en todos sus paths).

-- 1. Status válido (coincide con IncidentStatus.java)
ALTER TABLE incidents
    ADD CONSTRAINT chk_incident_status_valid
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'));

-- 2. Prioridad válida (coincide con IncidentPriority.java)
ALTER TABLE incidents
    ADD CONSTRAINT chk_incident_priority_valid
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

-- 3. Event type válido (coincide con IncidentEvent.java)
ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_event_type_valid
    CHECK (event_type IN ('INCIDENT_CREATED', 'INCIDENT_ASSIGNED', 'INCIDENT_STATUS_CHANGED'));

-- 4. Trigger: updated_at siempre refleja el último cambio
CREATE OR REPLACE FUNCTION fn_incidents_touch_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_incidents_touch_updated_at ON incidents;
CREATE TRIGGER trg_incidents_touch_updated_at
    BEFORE UPDATE ON incidents
    FOR EACH ROW
    EXECUTE FUNCTION fn_incidents_touch_updated_at();
