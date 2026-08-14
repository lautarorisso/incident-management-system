-- V3: Funciones SQL de apoyo: estadísticas, SLA y próximas acciones.
-- Mueven a la BD lógica de agregación/consulta que hoy vive en la app.

-- 1. fn_incident_stats(team_id, since): dashboard por team en ventana temporal.
CREATE OR REPLACE FUNCTION fn_incident_stats(
    p_team_id UUID,
    p_since   TIMESTAMPTZ
)
RETURNS TABLE(
    status   TEXT,
    priority TEXT,
    cnt      BIGINT,
    avg_age  INTERVAL
)
LANGUAGE sql
STABLE
PARALLEL SAFE
AS $$
    SELECT i.status,
           i.priority,
           COUNT(*)::BIGINT AS cnt,
           AVG(now() - i.created_at) AS avg_age
    FROM incidents i
    WHERE i.team_id = p_team_id
      AND i.created_at >= p_since
    GROUP BY i.status, i.priority;
$$;

-- 2. fn_incident_sla(incident_id): SLA por prioridad y próxima acción.
--   CRITICAL: 4h | HIGH: 24h | MEDIUM: 72h | LOW: 30d
--   next_action: breached -> ESCALATE | restante <=50% -> ACKNOWLEDGE | else MONITOR
CREATE OR REPLACE FUNCTION fn_incident_sla(
    p_incident_id UUID
)
RETURNS TABLE(
    breached    BOOLEAN,
    remaining   INTERVAL,
    next_action TEXT
)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_incident     incidents%ROWTYPE;
    v_sla_interval INTERVAL;
    v_elapsed      INTERVAL;
BEGIN
    SELECT * INTO v_incident FROM incidents WHERE id = p_incident_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Incident % not found', p_incident_id USING ERRCODE = 'P0001';
    END IF;

    v_sla_interval := CASE v_incident.priority
        WHEN 'CRITICAL' THEN INTERVAL '4 hours'
        WHEN 'HIGH'     THEN INTERVAL '24 hours'
        WHEN 'MEDIUM'   THEN INTERVAL '72 hours'
        WHEN 'LOW'      THEN INTERVAL '30 days'
        ELSE INTERVAL '24 hours'
    END;

    v_elapsed := now() - v_incident.created_at;
    breached    := v_elapsed > v_sla_interval;
    remaining   := v_sla_interval - v_elapsed;
    next_action := CASE
        WHEN breached THEN 'ESCALATE'
        WHEN v_elapsed >= (v_sla_interval * 0.5) THEN 'ACKNOWLEDGE'
        ELSE 'MONITOR'
    END;
    RETURN NEXT;
END;
$$;

-- 3. fn_next_action_for_assignee(user_id): "qué debo hacer ahora" para un assignee.
--    Solo OPEN/IN_PROGRESS, ordenado por urgencia SLA, top 5.
CREATE OR REPLACE FUNCTION fn_next_action_for_assignee(
    p_user_id UUID
)
RETURNS SETOF incidents
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    r incidents%ROWTYPE;
BEGIN
    FOR r IN
        SELECT i.*
        FROM incidents i
        CROSS JOIN LATERAL fn_incident_sla(i.id) sla
        WHERE i.assignee_id = p_user_id
          AND i.status IN ('OPEN', 'IN_PROGRESS')
        ORDER BY sla.breached DESC,
                 sla.remaining ASC,
                 CASE i.priority
                     WHEN 'CRITICAL' THEN 4
                     WHEN 'HIGH'     THEN 3
                     WHEN 'MEDIUM'   THEN 2
                     ELSE 1
                 END DESC,
                 i.created_at ASC
        LIMIT 5
    LOOP
        RETURN NEXT r;
    END LOOP;
END;
$$;
