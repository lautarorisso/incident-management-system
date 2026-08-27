-- V2: Seed del agente de soporte `agente1`, alineado con el Keycloak realm `ims`
-- (mismo keycloak_id que el import ims-realm.json para que el pipeline JWT→usuario funcione).
-- Idempotente: si el usuario ya existe, no se re-inserta.

INSERT INTO users (id, keycloak_id, username, display_name, email, active, created_at)
SELECT '9f6a2d1e-0000-4000-8000-0000000000A1',
       '9f6a2d1e-0000-4000-8000-000000000001',
       'agente1',
       'Agente Uno',
       'agente1@ims.local',
       true,
       now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'agente1');
