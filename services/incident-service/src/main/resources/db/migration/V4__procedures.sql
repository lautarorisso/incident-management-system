-- V4: Stored procedures para asignación y transición atómicas a nivel BD.
-- Herramientas de operación/auditoría; la app mantiene su flujo Java como
-- fuente de verdad (assign/transition con @Transactional + outbox).
--
-- La matriz de transiciones debe coincidir con IncidentStateMachine.java:
--   OPEN -> IN_PROGRESS
--   IN_PROGRESS -> RESOLVED
--   RESOLVED -> CLOSED | OPEN
--   CLOSED -> (ninguna)
--
-- Los event types deben coincidir con IncidentEvent.java:
--   INCIDENT_ASSIGNED, INCIDENT_STATUS_CHANGED

-- 1. sp_assign_incident
CREATE OR REPLACE PROCEDURE sp_assign_incident(
    p_incident_id UUID,
    p_assignee_id UUID,
    p_team_id     UUID
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_incident  incidents%ROWTYPE;
    v_sqlstate  TEXT;
    v_msg       TEXT;
BEGIN
    BEGIN
        SELECT * INTO v_incident
        FROM incidents
        WHERE id = p_incident_id
        FOR UPDATE SKIP LOCKED;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Incident % not found', p_incident_id USING ERRCODE = 'P0001';
        END IF;

        IF v_incident.status NOT IN ('OPEN', 'IN_PROGRESS') THEN
            RAISE EXCEPTION 'Cannot assign incident in status %', v_incident.status
                USING ERRCODE = 'P0002';
        END IF;

        UPDATE incidents
        SET assignee_id = p_assignee_id,
            team_id     = p_team_id,
            updated_at  = now()
        WHERE id = p_incident_id;

        INSERT INTO outbox_events (id, aggregate_id, event_type, payload, published, created_at)
        VALUES (gen_random_uuid(),
                p_incident_id,
                'INCIDENT_ASSIGNED',
                jsonb_build_object(
                    'incidentId', p_incident_id,
                    'assigneeId', p_assignee_id,
                    'teamId', p_team_id
                )::TEXT,
                false,
                now());

        SELECT * INTO v_incident FROM incidents WHERE id = p_incident_id;
        RAISE NOTICE 'Incident assigned: id=% assignee=% status=%', v_incident.id, v_incident.assignee_id, v_incident.status;
    EXCEPTION
        WHEN OTHERS THEN
            GET STACKED DIAGNOSTICS v_sqlstate = RETURNED_SQLSTATE, v_msg = MESSAGE_TEXT;
            RAISE EXCEPTION 'sp_assign_incident failed: % - %', v_sqlstate, v_msg
                USING ERRCODE = v_sqlstate;
    END;
END;
$$;

-- 2. sp_transition_incident
CREATE OR REPLACE PROCEDURE sp_transition_incident(
    p_incident_id UUID,
    p_new_status  TEXT,
    p_actor_id    UUID
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_incident  incidents%ROWTYPE;
    v_valid     BOOLEAN;
    v_sqlstate  TEXT;
    v_msg       TEXT;
BEGIN
    BEGIN
        SELECT * INTO v_incident
        FROM incidents
        WHERE id = p_incident_id
        FOR UPDATE SKIP LOCKED;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Incident % not found', p_incident_id USING ERRCODE = 'P0001';
        END IF;

        -- Matriz idéntica a IncidentStateMachine.java
        v_valid := CASE
            WHEN v_incident.status = 'OPEN'        AND p_new_status = 'IN_PROGRESS' THEN true
            WHEN v_incident.status = 'IN_PROGRESS' AND p_new_status = 'RESOLVED'    THEN true
            WHEN v_incident.status = 'RESOLVED'    AND p_new_status IN ('CLOSED', 'OPEN') THEN true
            ELSE false
        END;

        IF NOT v_valid THEN
            RAISE EXCEPTION 'Cannot transition from % to %', v_incident.status, p_new_status
                USING ERRCODE = 'P0003';
        END IF;

        UPDATE incidents
        SET status     = p_new_status,
            updated_at = now()
        WHERE id = p_incident_id;

        INSERT INTO outbox_events (id, aggregate_id, event_type, payload, published, created_at)
        VALUES (gen_random_uuid(),
                p_incident_id,
                'INCIDENT_STATUS_CHANGED',
                jsonb_build_object(
                    'incidentId', p_incident_id,
                    'fromStatus', v_incident.status,
                    'toStatus', p_new_status,
                    'actorId', p_actor_id
                )::TEXT,
                false,
                now());

        RAISE NOTICE 'Incident transitioned: id=% % -> %', v_incident.id, v_incident.status, p_new_status;
    EXCEPTION
        WHEN OTHERS THEN
            GET STACKED DIAGNOSTICS v_sqlstate = RETURNED_SQLSTATE, v_msg = MESSAGE_TEXT;
            RAISE EXCEPTION 'sp_transition_incident failed: % - %', v_sqlstate, v_msg
                USING ERRCODE = v_sqlstate;
    END;
END;
$$;
